package com.example.untitled.service.importer;

import com.example.untitled.entity.Book;
import com.example.untitled.service.impl.BookBatchWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * Reads all records, parses them with {@code parallelStream()} in a dedicated ForkJoinPool,
 * then inserts batches sequentially. Logs with the prefix {@code [ps]}.
 */
@Component
public class ParallelStreamImportStrategy extends AbstractImportStrategy {

    public static final String NAME = "parallel-stream";

    /** Threads used by {@code parallelStream()} for parsing. */
    private final int parseParallelism;

    /**
     * @param parseParallelism parsing threads; 0 or less means one per CPU core
     */
    public ParallelStreamImportStrategy(BookBatchWriter batchWriter,
                                        @Value("${app.import.parallel-stream.parallelism:0}") int parseParallelism) {
        super(batchWriter);
        this.parseParallelism = parseParallelism > 0 ? parseParallelism : Runtime.getRuntime().availableProcessors();
        log.info("Parallel-stream import parsing threads: {}", this.parseParallelism);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected ImportCounts doImport(BookReader reader) throws IOException {
        List<BookRecord> records = new ArrayList<>();
        BookRecord record;
        while ((record = reader.next()) != null) {
            records.add(record);
        }

        long parseStart = System.currentTimeMillis();
        List<Book> books = parseInDedicatedPool(records);
        log.info("[ps] Parsed {} records into {} books with {} threads in {} ms",
                records.size(), books.size(), parseParallelism, System.currentTimeMillis() - parseStart);

        long insertStart = System.currentTimeMillis();
        for (int from = 0; from < books.size(); from += BATCH_SIZE) {
            batchWriter.insert(books.subList(from, Math.min(from + BATCH_SIZE, books.size())));
        }
        log.info("[ps] Inserted {} books sequentially in {} ms", books.size(), System.currentTimeMillis() - insertStart);

        return new ImportCounts(books.size(), records.size() - books.size());
    }

    /**
     * Runs the parallel stream inside a dedicated ForkJoinPool so its thread count is configurable
     * ({@code app.import.parallel-stream.parallelism}) without changing the JVM-wide common pool.
     * A parallel stream started from inside a ForkJoinPool task executes in that pool.
     */
    private List<Book> parseInDedicatedPool(List<BookRecord> records) {
        ForkJoinPool.ForkJoinWorkerThreadFactory namedThreads = pool -> {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            thread.setName("import-parse-" + thread.getPoolIndex());
            return thread;
        };
        try (ForkJoinPool pool = new ForkJoinPool(parseParallelism, namedThreads, null, false)) {
            return pool.submit(() -> records.parallelStream()
                            .map(BookRecord::toBook)
                            .filter(Objects::nonNull)
                            .toList())
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Parsing interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Parsing failed", e.getCause());
        }
    }
}
