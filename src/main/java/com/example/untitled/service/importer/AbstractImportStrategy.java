package com.example.untitled.service.importer;

import com.example.untitled.service.ImportResult;
import com.example.untitled.service.impl.BookBatchWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Template Method pattern: {@link #run(ImportJob)} fixes the steps every import shares
 * (open reader → import → measure → log → close reader); subclasses only implement {@link #doImport(BookReader)}.
 */
public abstract class AbstractImportStrategy implements ImportStrategy {

    /** Rows per {@link BookBatchWriter#insert} call, i.e. per transaction. */
    protected static final int BATCH_SIZE = 5000;

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final BookBatchWriter batchWriter;

    protected AbstractImportStrategy(BookBatchWriter batchWriter) {
        this.batchWriter = batchWriter;
    }

    @Override
    public final ImportResult run(ImportJob job) throws IOException {
        long startTime = System.currentTimeMillis();
        log.info("Starting import: strategy={}, filename={}, format={}, sizeBytes={}",
                name(), job.filename(), job.format(), job.sizeBytes());
        try (BookReader reader = job.openReader()) {
            ImportCounts counts = doImport(reader);
            ImportResult result = new ImportResult(counts.imported(), counts.skipped(), System.currentTimeMillis() - startTime);
            double seconds = Math.max(result.durationMs(), 1) / 1000.0;
            log.info("Import completed: strategy={}, filename={}, imported={}, skipped={}, durationMs={}, booksPerSec={}",
                    name(), job.filename(), result.imported(), result.skipped(), result.durationMs(),
                    java.lang.String.format("%.2f", result.imported() / seconds));
            return result;
        } catch (IOException | RuntimeException e) {
            log.error("Import failed: strategy={}, filename={}", name(), job.filename(), e);
            throw e;
        }
    }

    /**
     * Reads every record from {@code reader}, inserts the valid ones and counts the invalid ones.
     */
    protected abstract ImportCounts doImport(BookReader reader) throws IOException;

    /**
     * @param imported rows inserted into the database
     * @param skipped  non-blank records that were not valid books
     */
    protected record ImportCounts(long imported, long skipped) {
    }
}
