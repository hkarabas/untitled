package com.example.untitled.service.importer;

import com.example.untitled.entity.Book;
import com.example.untitled.service.impl.BookBatchWriter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Pipeline edge cases that the end-to-end tests in {@link BookImportServiceTest} cannot reach deterministically.
 * Calls {@code doImport} directly with an in-memory reader.
 */
class VirtualThreadsImportStrategyTest {

    private static final int BATCH = AbstractImportStrategy.BATCH_SIZE;

    private final BookBatchWriter writer = mock(BookBatchWriter.class);
    private final VirtualThreadsImportStrategy strategy = new VirtualThreadsImportStrategy(writer);

    @Test
    void exactMultipleOfBatchSizeHasNoPartialBatch() throws Exception {
        AbstractImportStrategy.ImportCounts counts = strategy.doImport(new InMemoryReader(2 * BATCH));

        assertThat(counts.imported()).isEqualTo(2L * BATCH);
        verify(writer, times(2)).insert(anyList());
    }

    @Test
    void errorThrownByABatchStillFailsTheImport() {
        doThrow(new AssertionError("not an Exception")).when(writer).insert(anyList());

        assertThatThrownBy(() -> strategy.doImport(new InMemoryReader(10)))
                .isInstanceOf(IllegalStateException.class)
                .cause()
                .hasMessage("Batch 1 did not finish");
    }

    @Test
    void batchSubmittedAfterAFailureIsSkipped() {
        int writers = VirtualThreadsImportStrategy.DB_WRITERS;
        CountDownLatch allWritersBusy = new CountDownLatch(writers);
        CountDownLatch fail = new CountDownLatch(1);
        List<Thread> writerThreads = new CopyOnWriteArrayList<>();
        doAnswer(inv -> {
            writerThreads.add(Thread.currentThread());
            allWritersBusy.countDown();
            fail.await();
            throw new RuntimeException("db down");
        }).when(writer).insert(anyList());

        // Before handing out the last record of batch writers+1, let every running batch fail and finish,
        // so that batch starts after the failure is recorded
        InMemoryReader reader = new InMemoryReader((writers + 1) * BATCH, () -> {
            allWritersBusy.await();
            fail.countDown();
            for (Thread thread : writerThreads) {
                thread.join();
            }
        });

        assertThatThrownBy(() -> strategy.doImport(reader))
                .isInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("db down");
        verify(writer, times(writers)).insert(anyList());
    }

    @Test
    void interruptWhileWaitingForBatchesFailsTheImport() {
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> strategy.doImport(new InMemoryReader(10)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Bulk operation interrupted");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted(); // clear the flag for the next tests
        }
    }

    @Test
    void rejectedBatchFailsTheImport() {
        VirtualThreadsImportStrategy rejecting = new VirtualThreadsImportStrategy(writer) {
            @Override
            ExecutorService newBatchExecutor() {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.shutdown(); // every execute() is now rejected
                return executor;
            }
        };

        assertThatThrownBy(() -> rejecting.doImport(new InMemoryReader(10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Batch 1 could not be submitted");
        verify(writer, never()).insert(anyList());
    }

    /** Returns {@code count} valid records; runs {@code beforeLast} just before returning the last one. */
    private static final class InMemoryReader implements BookReader {

        private final int count;
        private final Hook beforeLast;
        private int position;

        InMemoryReader(int count) {
            this(count, () -> { });
        }

        InMemoryReader(int count, Hook beforeLast) {
            this.count = count;
            this.beforeLast = beforeLast;
        }

        @Override
        public BookRecord next() {
            if (position == count) {
                return null;
            }
            if (++position == count) {
                try {
                    beforeLast.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
            return () -> new Book("Title", "Author", BigDecimal.ONE);
        }

        @Override
        public long position() {
            return position;
        }

        @Override
        public void close() {
            // nothing to release
        }
    }

    @FunctionalInterface
    private interface Hook {
        void run() throws InterruptedException;
    }
}
