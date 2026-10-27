package com.example.untitled.service.importer;

import com.example.untitled.service.impl.BookBatchWriter;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Parsing failures, which the end-to-end tests in {@link BookImportServiceTest} cannot produce:
 * real readers return records whose {@code toBook()} never throws.
 */
class ParallelStreamImportStrategyTest {

    private final ParallelStreamImportStrategy strategy = new ParallelStreamImportStrategy(mock(BookBatchWriter.class), 1);

    @Test
    void recordThatThrowsFailsParsing() {
        BookRecord broken = () -> {
            throw new IllegalArgumentException("broken record");
        };

        assertThatThrownBy(() -> strategy.doImport(reader(broken)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Parsing failed")
                .hasRootCauseMessage("broken record");
    }

    @Test
    void interruptWhileParsingFailsTheImport() {
        BookRecord slow = () -> {
            LockSupport.parkNanos(Duration.ofMillis(200).toNanos()); // still running when get() is called
            return null;
        };

        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> strategy.doImport(reader(slow)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Parsing interrupted");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted(); // clear the flag for the next tests
        }
    }

    private static BookReader reader(BookRecord... records) {
        Iterator<BookRecord> iterator = List.of(records).iterator();
        return new BookReader() {
            @Override
            public BookRecord next() {
                return iterator.hasNext() ? iterator.next() : null;
            }

            @Override
            public long position() {
                return 0;
            }

            @Override
            public void close() {
                // nothing to release
            }
        };
    }
}
