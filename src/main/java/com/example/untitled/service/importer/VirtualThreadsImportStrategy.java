package com.example.untitled.service.importer;

import com.example.untitled.entity.Book;
import com.example.untitled.service.impl.BookBatchWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Streaming pipeline: the calling thread reads records and hands every {@value #BATCH_SIZE}-record batch
 * to a new virtual thread, which parses and inserts it. At most {@value #DB_WRITERS} batches are in flight;
 * when all writers are busy the reader blocks (backpressure), so memory stays bounded regardless of file size
 * and reading overlaps with DB writes. The first failure stops reading and fails the import.
 * <p>
 * Logs every step with the prefix {@code [vt]} (per-batch details at DEBUG).
 */
@Component
public class VirtualThreadsImportStrategy extends AbstractImportStrategy {

    public static final String NAME = "virtual-threads";

    /**
     * Concurrent DB writers. Kept below the Hikari pool size (30) so regular API requests still get connections.
     */
    static final int DB_WRITERS = 20;

    private static final String TAG = "[vt]";

    public VirtualThreadsImportStrategy(BookBatchWriter batchWriter) {
        super(batchWriter);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected ImportCounts doImport(BookReader reader) throws IOException {
        long startTime = System.currentTimeMillis();
        AtomicLong imported = new AtomicLong();
        AtomicLong skipped = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Semaphore writers = new Semaphore(DB_WRITERS);
        int batches = 0;
        long chunkFirstPosition = 1;

        log.info(TAG + " Pipeline started: batchSize={}, maxConcurrentWriters={}", BATCH_SIZE, DB_WRITERS);

        // Virtual threads are cheap and not pooled; the semaphore is what limits concurrency.
        // Named import-batch-1, import-batch-2, ... (one thread per batch, so the number matches the batch number)
        ThreadFactory batchThreads = Thread.ofVirtual().name("import-batch-", 1).factory();
        try (ExecutorService executor = Executors.newThreadPerTaskExecutor(batchThreads)) {
            List<BookRecord> chunk = new ArrayList<>(BATCH_SIZE);
            BookRecord record;
            while (failure.get() == null && (record = reader.next()) != null) {
                chunk.add(record);
                if (chunk.size() == BATCH_SIZE) {
                    batches++;
                    log.debug(TAG + " Batch {} read: {} records (file lines/rows {}-{})",
                            batches, chunk.size(), chunkFirstPosition, reader.position());
                    submitBatch(executor, writers, chunk, batches, imported, skipped, failure);
                    chunk = new ArrayList<>(BATCH_SIZE);
                    chunkFirstPosition = reader.position() + 1;
                }
            }
            if (failure.get() != null) {
                log.warn(TAG + " Reading stopped early at file line/row {} because a batch failed", reader.position());
            } else if (!chunk.isEmpty()) {
                batches++;
                log.debug(TAG + " Batch {} read (last, partial): {} records (file lines/rows {}-{})",
                        batches, chunk.size(), chunkFirstPosition, reader.position());
                submitBatch(executor, writers, chunk, batches, imported, skipped, failure);
            }

            int inFlight = DB_WRITERS - writers.availablePermits();
            log.info(TAG + " Finished reading: {} file lines/rows, {} batches submitted in {} ms; waiting for {} in-flight batches",
                    reader.position(), batches, System.currentTimeMillis() - startTime, inFlight);
            // Wait for all in-flight batches by draining every permit
            writers.acquire(DB_WRITERS);
            log.info(TAG + " All batches finished");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(TAG + " Import interrupted after {} batches", batches);
            throw new IllegalStateException("Bulk operation interrupted", e);
        }

        if (failure.get() != null) {
            log.error(TAG + " Import failed: imported={}, skipped={} before failure", imported.get(), skipped.get());
            throw new IllegalStateException("Import failed after " + imported.get() + " books were imported",
                    failure.get());
        }
        return new ImportCounts(imported.get(), skipped.get());
    }

    private void submitBatch(ExecutorService executor, Semaphore writers, List<BookRecord> chunk, int batchNo,
                             AtomicLong imported, AtomicLong skipped, AtomicReference<Throwable> failure)
            throws InterruptedException {
        // Blocks the reader while DB_WRITERS batches are in flight (backpressure)
        if (!writers.tryAcquire()) {
            long waitStart = System.currentTimeMillis();
            log.debug(TAG + " Batch {} waiting: all {} writers busy", batchNo, DB_WRITERS);
            writers.acquire();
            log.debug(TAG + " Batch {} got a writer after waiting {} ms", batchNo, System.currentTimeMillis() - waitStart);
        }
        try {
            executor.execute(() -> {
                try {
                    if (failure.get() != null) {
                        log.warn(TAG + " Batch {} skipped: an earlier batch already failed", batchNo);
                        return;
                    }
                    log.debug(TAG + " Batch {} started on {}", batchNo, Thread.currentThread());

                    long parseStart = System.currentTimeMillis();
                    List<Book> batch = new ArrayList<>(chunk.size());
                    int invalid = 0;
                    for (BookRecord record : chunk) {
                        Book book = record.toBook();
                        if (book == null) {
                            invalid++;
                        } else {
                            batch.add(book);
                        }
                    }
                    skipped.addAndGet(invalid);
                    log.debug(TAG + " Batch {} parsed: {} valid, {} invalid in {} ms",
                            batchNo, batch.size(), invalid, System.currentTimeMillis() - parseStart);

                    long insertStart = System.currentTimeMillis();
                    batchWriter.insert(batch);
                    long total = imported.addAndGet(batch.size());
                    log.info(TAG + " Batch {} inserted: {} books in {} ms (total imported so far: {})",
                            batchNo, batch.size(), System.currentTimeMillis() - insertStart, total);
                } catch (Throwable t) {
                    log.error(TAG + " Batch {} failed: {}", batchNo, t.toString());
                    failure.compareAndSet(null, t);
                } finally {
                    writers.release();
                }
            });
        } catch (RuntimeException e) {
            writers.release();
            log.error(TAG + " Batch {} could not be submitted", batchNo, e);
            throw e;
        }
    }
}
