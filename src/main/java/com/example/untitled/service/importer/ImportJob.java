package com.example.untitled.service.importer;

import com.example.untitled.service.BulkOperationGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * One uploaded file waiting to be imported. Owns the temp copy of the upload and the bulk-operation permit;
 * whoever runs the job closes it, which deletes the file and releases the permit. This lets the same job
 * run inside the request or on a background thread.
 */
public final class ImportJob implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ImportJob.class);

    private final String filename;
    private final ImportFormat format;
    private final Path file;
    private final long sizeBytes;
    private final BookReaderFactory readerFactory;
    private final BulkOperationGuard.Permit permit;

    ImportJob(String filename, ImportFormat format, Path file, long sizeBytes,
              BookReaderFactory readerFactory, BulkOperationGuard.Permit permit) {
        this.filename = filename;
        this.format = format;
        this.file = file;
        this.sizeBytes = sizeBytes;
        this.readerFactory = readerFactory;
        this.permit = permit;
    }

    public String filename() {
        return filename;
    }

    public ImportFormat format() {
        return format;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    public BookReader openReader() throws IOException {
        return readerFactory.open(format, file);
    }

    @Override
    public void close() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete temporary import file {}", file, e);
        } finally {
            permit.close();
        }
    }
}
