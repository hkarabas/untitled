package com.example.untitled.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.untitled.entity.Book;
import com.example.untitled.repository.BookRepository;
import com.example.untitled.service.BookService;
import com.example.untitled.service.BulkOperationInProgressException;
import com.example.untitled.service.ImportResult;

@Service
public class BookServiceImpl implements BookService {

    private static final Logger log = LoggerFactory.getLogger(BookServiceImpl.class);

    public static final int MAX_GENERATED_BOOKS = 1_000_000;

    private static final int BATCH_SIZE = 5000;

    /**
     * Concurrent DB writers per bulk operation. Kept below the Hikari pool size (30)
     * so regular API requests still get connections during an import.
     */
    private static final int DB_WRITERS = 20;

    private static final String[] FIRST_NAMES = {"John", "Jane", "Michael", "Emily", "David", "Sarah", "Robert", "Lisa",
            "James", "Mary", "William", "Patricia", "Richard", "Jennifer", "Charles"};
    private static final String[] LAST_NAMES = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller",
            "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson"};
    private static final String[] ADJECTIVES = {"Great", "Amazing", "Wonderful", "Fantastic", "Incredible", "Mysterious",
            "Hidden", "Lost", "Ancient", "Modern", "Secret", "Dark", "Bright", "Silent"};
    private static final String[] NOUNS = {"Adventure", "Journey", "Story", "Tale", "Mystery", "Dream", "Legend", "Quest",
            "Chronicle", "Saga", "Odyssey", "Treasure", "Kingdom", "Empire", "World"};

    private final BookRepository bookRepository;
    private final BookBatchWriter batchWriter;

    /** Only one bulk operation runs at a time, so they cannot exhaust the connection pool or heap together. */
    private final Semaphore bulkPermit = new Semaphore(1);

    public BookServiceImpl(BookRepository bookRepository, BookBatchWriter batchWriter) {
        this.bookRepository = bookRepository;
        this.batchWriter = batchWriter;
    }

    @Override
    public Page<Book> findPage(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    @Override
    public Optional<Book> findById(Long id) {
        return bookRepository.findById(id);
    }

    @Override
    public Book save(Book book) {
        return bookRepository.save(book);
    }

    @Override
    public boolean deleteById(Long id) {
        if (!bookRepository.existsById(id)) {
            return false;
        }
        bookRepository.deleteById(id);
        return true;
    }

    @Override
    public void generateRandomBooks(int count) {
        if (count < 1 || count > MAX_GENERATED_BOOKS) {
            throw new IllegalArgumentException("count must be between 1 and " + MAX_GENERATED_BOOKS);
        }
        acquireBulkPermit();
        try {
            long startTime = System.currentTimeMillis();
            List<Runnable> tasks = new ArrayList<>();
            for (int startIdx = 0; startIdx < count; startIdx += BATCH_SIZE) {
                int from = startIdx;
                int to = Math.min(startIdx + BATCH_SIZE, count);
                tasks.add(() -> batchWriter.insert(randomBooks(from, to)));
            }
            log.info("Generating {} random books in {} batches", count, tasks.size());
            runConcurrently(tasks);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Generated {} books in {} ms", count, duration);
        } finally {
            bulkPermit.release();
        }
    }

    private static List<Book> randomBooks(int from, int to) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Book> books = new ArrayList<>(to - from);
        for (int i = from; i < to; i++) {
            String author = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                    + LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String title = "The " + ADJECTIVES[random.nextInt(ADJECTIVES.length)] + " "
                    + NOUNS[random.nextInt(NOUNS.length)] + " " + (i + 1);
            // Random price between 5.99 and 99.99
            BigDecimal price = BigDecimal.valueOf(5.99 + random.nextDouble() * 94.00).setScale(2, RoundingMode.HALF_UP);
            books.add(new Book(title, author, price));
        }
        return books;
    }

    /**
     * Single-threaded streaming import: reads line by line and inserts every {@value #BATCH_SIZE} rows.
     * Baseline for performance comparison.
     */
    @Override
    public ImportResult importCsvTraditional(MultipartFile file) throws IOException {
        String filename = validateCsv(file);
        acquireBulkPermit();
        try {
            long startTime = System.currentTimeMillis();
            log.info("Starting CSV import: strategy=traditional, filename={}, sizeBytes={}", filename, file.getSize());

            long imported = 0;
            long skipped = 0;
            List<Book> batch = new ArrayList<>(BATCH_SIZE);
            try (BufferedReader reader = reader(file.getInputStream())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (CsvBookParser.isBlank(line)) {
                        continue;
                    }
                    Book book = CsvBookParser.parse(line);
                    if (book == null) {
                        skipped++;
                        continue;
                    }
                    batch.add(book);
                    if (batch.size() >= BATCH_SIZE) {
                        batchWriter.insert(batch);
                        imported += batch.size();
                        batch = new ArrayList<>(BATCH_SIZE);
                    }
                }
            }
            batchWriter.insert(batch);
            imported += batch.size();

            return logResult("traditional", filename,
                    new ImportResult(imported, skipped, System.currentTimeMillis() - startTime));
        } finally {
            bulkPermit.release();
        }
    }

    /**
     * Streaming pipeline: the request thread reads the upload and hands every {@value #BATCH_SIZE}-line batch
     * to a new virtual thread, which parses and inserts it. At most {@value #DB_WRITERS} batches are in flight;
     * when all writers are busy the reader blocks (backpressure), so memory stays bounded regardless of file size
     * and reading overlaps with DB writes. The first failure stops reading and fails the import.
     */
    @Override
    public ImportResult importCsvVirtualThreads(MultipartFile file) throws IOException {
        String filename = validateCsv(file);
        acquireBulkPermit();
        try {
            long startTime = System.currentTimeMillis();
            log.info("Starting CSV import: strategy=virtual-threads, filename={}, sizeBytes={}", filename, file.getSize());
            try (BufferedReader reader = reader(file.getInputStream())) {
                return logResult("virtual-threads", filename, streamBatchesConcurrently(reader, startTime));
            }
        } finally {
            bulkPermit.release();
        }
    }

    private ImportResult streamBatchesConcurrently(BufferedReader reader, long startTime) throws IOException {
        AtomicLong imported = new AtomicLong();
        AtomicLong skipped = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Semaphore writers = new Semaphore(DB_WRITERS);
        int batches = 0;

        // Virtual threads are cheap and not pooled; the semaphore is what limits concurrency
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<String> chunk = new ArrayList<>(BATCH_SIZE);
            String line;
            while (failure.get() == null && (line = reader.readLine()) != null) {
                if (CsvBookParser.isBlank(line)) {
                    continue;
                }
                chunk.add(line);
                if (chunk.size() == BATCH_SIZE) {
                    submitBatch(executor, writers, chunk, imported, skipped, failure);
                    chunk = new ArrayList<>(BATCH_SIZE);
                    batches++;
                }
            }
            if (!chunk.isEmpty() && failure.get() == null) {
                submitBatch(executor, writers, chunk, imported, skipped, failure);
                batches++;
            }
            // Wait for all in-flight batches by draining every permit
            writers.acquire(DB_WRITERS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bulk operation interrupted", e);
        }

        if (failure.get() != null) {
            throw new IllegalStateException("CSV import failed after " + imported.get() + " books were imported",
                    failure.get());
        }
        log.debug("Imported {} batches", batches);
        return new ImportResult(imported.get(), skipped.get(), System.currentTimeMillis() - startTime);
    }

    private void submitBatch(ExecutorService executor, Semaphore writers, List<String> chunk,
                             AtomicLong imported, AtomicLong skipped, AtomicReference<Throwable> failure)
            throws InterruptedException {
        writers.acquire(); // blocks the reader while DB_WRITERS batches are in flight
        try {
            executor.execute(() -> {
                try {
                    if (failure.get() != null) {
                        return; // another batch already failed; don't write more
                    }
                    List<Book> batch = new ArrayList<>(chunk.size());
                    for (String csvLine : chunk) {
                        Book book = CsvBookParser.parse(csvLine);
                        if (book == null) {
                            skipped.incrementAndGet();
                        } else {
                            batch.add(book);
                        }
                    }
                    batchWriter.insert(batch);
                    imported.addAndGet(batch.size());
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                } finally {
                    writers.release();
                }
            });
        } catch (RuntimeException e) {
            writers.release();
            throw e;
        }
    }

    /**
     * Copies the upload to a temp file (the multipart file is deleted when the request ends),
     * then imports it on a background virtual thread and returns immediately.
     * Failures are only visible in the logs.
     */
    @Override
    public void importCsvParallelFireAndForget(MultipartFile file) throws IOException {
        String filename = validateCsv(file);
        acquireBulkPermit();
        Path tempFile = null;
        try {
            log.info("Starting CSV import: strategy=parallel-fire-and-forget, filename={}, sizeBytes={}",
                    filename, file.getSize());
            tempFile = Files.createTempFile("books_upload_parallel_", ".csv");
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            Path csv = tempFile;
            Thread.ofVirtual().name("csv-import-" + filename).start(() -> runBackgroundImport(csv, filename));
        } catch (IOException | RuntimeException e) {
            // Background thread never started: clean up here
            deleteQuietly(tempFile);
            bulkPermit.release();
            throw e;
        }
    }

    private void runBackgroundImport(Path csv, String filename) {
        long startTime = System.currentTimeMillis();
        try {
            List<String> lines = Files.readAllLines(csv);
            ImportResult result = importLinesConcurrently(lines, calculateOptimalChunkSize(lines.size()), startTime);
            logResult("parallel-fire-and-forget", filename, result);
        } catch (Exception e) {
            log.error("Background CSV import failed: strategy=parallel-fire-and-forget, filename={}", filename, e);
        } finally {
            deleteQuietly(csv);
            bulkPermit.release();
        }
    }

    /**
     * Parses all lines with {@code parallelStream()} (ForkJoinPool), then inserts batches sequentially.
     */
    @Override
    public ImportResult importCsvParallelStream(MultipartFile file) throws IOException {
        String filename = validateCsv(file);
        acquireBulkPermit();
        try {
            long startTime = System.currentTimeMillis();
            log.info("Starting CSV import: strategy=parallel-stream, filename={}, sizeBytes={}", filename, file.getSize());

            List<String> lines = readLines(file.getInputStream()).stream()
                    .filter(line -> !CsvBookParser.isBlank(line))
                    .toList();
            List<Book> books = lines.parallelStream()
                    .map(CsvBookParser::parse)
                    .filter(Objects::nonNull)
                    .toList();

            for (int from = 0; from < books.size(); from += BATCH_SIZE) {
                batchWriter.insert(books.subList(from, Math.min(from + BATCH_SIZE, books.size())));
            }

            return logResult("parallel-stream", filename, new ImportResult(
                    books.size(), lines.size() - books.size(), System.currentTimeMillis() - startTime));
        } finally {
            bulkPermit.release();
        }
    }

    private ImportResult importLinesConcurrently(List<String> lines, int chunkSize, long startTime) {
        AtomicLong imported = new AtomicLong();
        AtomicLong skipped = new AtomicLong();
        List<Runnable> tasks = new ArrayList<>();

        for (int startIdx = 0; startIdx < lines.size(); startIdx += chunkSize) {
            List<String> chunk = lines.subList(startIdx, Math.min(startIdx + chunkSize, lines.size()));
            tasks.add(() -> {
                List<Book> batch = new ArrayList<>(chunk.size());
                for (String line : chunk) {
                    if (CsvBookParser.isBlank(line)) {
                        continue;
                    }
                    Book book = CsvBookParser.parse(line);
                    if (book == null) {
                        skipped.incrementAndGet();
                    } else {
                        batch.add(book);
                    }
                }
                batchWriter.insert(batch);
                imported.addAndGet(batch.size());
            });
        }

        runConcurrently(tasks);
        return new ImportResult(imported.get(), skipped.get(), System.currentTimeMillis() - startTime);
    }

    /**
     * Runs tasks on at most {@value #DB_WRITERS} virtual threads and waits for all of them.
     * If any task fails, the remaining ones are cancelled and the failure is rethrown.
     */
    private void runConcurrently(List<Runnable> tasks) {
        AtomicInteger completed = new AtomicInteger();
        try (ExecutorService executor = Executors.newFixedThreadPool(DB_WRITERS, Thread.ofVirtual().factory())) {
            List<Future<?>> futures = new ArrayList<>(tasks.size());
            for (Runnable task : tasks) {
                futures.add(executor.submit(() -> {
                    task.run();
                    int done = completed.incrementAndGet();
                    if (done % 20 == 0 || done == tasks.size()) {
                        log.debug("Progress: {}/{} batches", done, tasks.size());
                    }
                }));
            }
            for (int i = 0; i < futures.size(); i++) {
                try {
                    futures.get(i).get();
                } catch (ExecutionException e) {
                    executor.shutdownNow();
                    throw new IllegalStateException("Batch " + (i + 1) + " of " + tasks.size() + " failed", e.getCause());
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Bulk operation interrupted", e);
                }
            }
        }
    }

    /**
     * Aim for ~30 chunks for good parallelism, but keep chunks between 1000 and 10000 lines.
     */
    private static int calculateOptimalChunkSize(int totalLines) {
        int minChunkSize = 1000;
        int maxChunkSize = 10000;
        int targetChunks = 30;
        return Math.min(Math.max(minChunkSize, totalLines / targetChunks), maxChunkSize);
    }

    private void acquireBulkPermit() {
        if (!bulkPermit.tryAcquire()) {
            throw new BulkOperationInProgressException();
        }
    }

    private static String validateCsv(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Please select a CSV file to upload");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files are allowed");
        }
        return filename;
    }

    private static BufferedReader reader(InputStream in) {
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    private static List<String> readLines(InputStream in) throws IOException {
        try (BufferedReader reader = reader(in)) {
            return reader.lines().toList();
        }
    }

    private static ImportResult logResult(String strategy, String filename, ImportResult result) {
        double seconds = Math.max(result.durationMs(), 1) / 1000.0;
        log.info("CSV import completed: strategy={}, filename={}, imported={}, skipped={}, durationMs={}, booksPerSec={}",
                strategy, filename, result.imported(), result.skipped(), result.durationMs(),
                java.lang.String.format("%.2f", result.imported() / seconds));
        return result;
    }

    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete temporary CSV file {}", file, e);
        }
    }
}
