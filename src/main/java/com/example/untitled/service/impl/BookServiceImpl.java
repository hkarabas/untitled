package com.example.untitled.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.untitled.entity.Book;
import com.example.untitled.repository.BookRepository;
import com.example.untitled.service.BookService;
import com.example.untitled.service.BulkOperationGuard;
import com.example.untitled.service.RandomBooks;

@Service
public class BookServiceImpl implements BookService {

    private static final Logger log = LoggerFactory.getLogger(BookServiceImpl.class);

    public static final int MAX_GENERATED_BOOKS = 1_000_000;

    private static final int BATCH_SIZE = 5000;

    /**
     * Concurrent DB writers for generation. Kept below the Hikari pool size (30)
     * so regular API requests still get connections.
     */
    private static final int DB_WRITERS = 20;

    private final BookRepository bookRepository;
    private final BookBatchWriter batchWriter;
    private final BulkOperationGuard guard;

    public BookServiceImpl(BookRepository bookRepository, BookBatchWriter batchWriter, BulkOperationGuard guard) {
        this.bookRepository = bookRepository;
        this.batchWriter = batchWriter;
        this.guard = guard;
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
        try (var _ = guard.acquire()) {
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
        }
    }

    private static List<Book> randomBooks(int from, int to) {
        List<Book> books = new ArrayList<>(to - from);
        for (long number = from + 1L; number <= to; number++) {
            books.add(RandomBooks.create(number));
        }
        return books;
    }

    /**
     * Runs tasks on at most {@value #DB_WRITERS} virtual threads and waits for all of them.
     * If any task fails, the remaining ones are cancelled and the failure is rethrown.
     */
    private void runConcurrently(List<Runnable> tasks) {
        AtomicInteger completed = new AtomicInteger();
        try (ExecutorService executor = Executors.newFixedThreadPool(DB_WRITERS, Thread.ofVirtual().factory())) {
            List<Future<?>> futures = tasks.stream()
                    .<Future<?>>map(task -> executor.submit(() -> {
                        task.run();
                        int done = completed.incrementAndGet();
                        if (done % 20 == 0 || done == tasks.size()) {
                            log.debug("Progress: {}/{} batches", done, tasks.size());
                        }
                    }))
                    .toList();
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
}
