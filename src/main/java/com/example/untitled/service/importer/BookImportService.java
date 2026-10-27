package com.example.untitled.service.importer;

import com.example.untitled.service.BulkOperationGuard;
import com.example.untitled.service.ImportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facade for importing books from uploaded files. Hides validation, locking, temp files and threading
 * behind two calls: {@link #importNow} and {@link #importInBackground}. Each endpoint passes the one
 * {@link ImportFormat} it accepts.
 * <p>
 * Any {@link ImportStrategy} works with any {@link ImportFormat}: the strategy decides how records are
 * processed, the {@link BookReaderFactory} decides how the file is read.
 */
@Service
public class BookImportService {

    private static final Logger log = LoggerFactory.getLogger(BookImportService.class);

    private static final FileAttribute<Set<PosixFilePermission>> OWNER_ONLY =
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    private static final boolean POSIX = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    private final Map<String, ImportStrategy> strategies = new LinkedHashMap<>();
    private final BookReaderFactory readerFactory;
    private final BulkOperationGuard guard;

    /**
     * @param strategies every {@link ImportStrategy} bean; Spring injects them all, so a new strategy
     *                   only needs a new {@code @Component} class
     */
    public BookImportService(List<ImportStrategy> strategies, BookReaderFactory readerFactory, BulkOperationGuard guard) {
        for (ImportStrategy strategy : strategies) {
            if (this.strategies.putIfAbsent(strategy.name(), strategy) != null) {
                throw new IllegalStateException("Duplicate import strategy name: " + strategy.name());
            }
        }
        this.readerFactory = readerFactory;
        this.guard = guard;
        log.info("Import strategies: {}", this.strategies.keySet());
    }

    public Set<String> strategyNames() {
        return strategies.keySet();
    }

    /**
     * Imports the file inside the current request and returns the result.
     *
     * @param format the format this endpoint accepts; a file with another extension is rejected
     */
    public ImportResult importNow(MultipartFile file, ImportFormat format, String strategyName) throws IOException {
        ImportStrategy strategy = strategy(strategyName);
        try (ImportJob job = prepare(file, format)) {
            return strategy.run(job);
        }
    }

    /**
     * Fire-and-forget: saves the upload, starts the import on a background virtual thread and returns immediately.
     * The outcome is only logged.
     *
     * @param format the format this endpoint accepts; a file with another extension is rejected
     */
    public void importInBackground(MultipartFile file, ImportFormat format, String strategyName) throws IOException {
        ImportStrategy strategy = strategy(strategyName);
        String name = strategy.name();
        ImportJob job = prepare(file, format);
        String filename = job.filename();
        try {
            startInBackground("import-background-" + name, () -> {
                try (job) {
                    log.info("[bg] Background import started: strategy={}, filename={}", name, filename);
                    strategy.run(job);
                } catch (Exception e) {
                    // Details were already logged by the strategy
                    log.error("[bg] Background import failed: strategy={}, filename={}: {}", name, filename, e.toString());
                }
            });
        } catch (RuntimeException e) {
            job.close(); // thread never started
            throw e;
        }
        log.info("[bg] Background import accepted: strategy={}, filename={}", name, filename);
    }

    /** Starts {@code task} on a new virtual thread. Package-private so tests can simulate a thread that fails to start. */
    void startInBackground(String threadName, Runnable task) {
        Thread.ofVirtual().name(threadName).start(task);
    }

    private ImportStrategy strategy(String name) {
        ImportStrategy strategy = strategies.get(name);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown import strategy '" + name + "'. Use one of " + strategies.keySet());
        }
        return strategy;
    }

    /**
     * Validates the upload, takes the bulk-operation permit and moves the upload to a temp file
     * (the multipart file is deleted when the request ends, and the Excel reader needs a real file).
     */
    private ImportJob prepare(MultipartFile file, ImportFormat format) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Please select a " + format.extension() + " file to upload");
        }
        String filename = file.getOriginalFilename();
        format.requireFilename(filename);

        BulkOperationGuard.Permit permit = guard.acquire();
        try {
            Path tempFile = copyToPrivateTempFile(file, format);
            return new ImportJob(filename, format, tempFile, file.getSize(), readerFactory, permit);
        } catch (IOException | RuntimeException e) {
            permit.close();
            throw e;
        }
    }

    /** Moves the upload to a new private temp file; deletes the temp file again if that fails. */
    private static Path copyToPrivateTempFile(MultipartFile file, ImportFormat format) throws IOException {
        Path tempFile = createPrivateTempFile(format.extension(), POSIX);
        try {
            // transferTo(File) lets Tomcat move its already-on-disk upload instead of copying the bytes
            file.transferTo(tempFile.toFile());
            return tempFile;
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    /**
     * Creates the temp file readable and writable by the app user only (rw-------), so other users
     * of the shared temp directory cannot read uploaded data.
     *
     * @param posix whether the default file system supports POSIX permissions ({@link #POSIX})
     */
    static Path createPrivateTempFile(String suffix, boolean posix) throws IOException {
        if (posix) {
            return Files.createTempFile("books_import_", suffix, OWNER_ONLY);
        }
        // Not POSIX (Windows): the default temp directory is per user and its ACL already limits access
        return Files.createTempFile("books_import_", suffix); // NOSONAR java:S5443 (per-user temp dir)
    }
}
