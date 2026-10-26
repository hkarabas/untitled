package com.example.untitled.service.impl;

import com.example.untitled.repository.BookRepository;
import com.example.untitled.service.BulkOperationGuard;
import com.example.untitled.service.BulkOperationInProgressException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BookServiceImplTest {

    private final BookBatchWriter writer = mock(BookBatchWriter.class);
    private final BulkOperationGuard guard = new BulkOperationGuard();
    private final BookServiceImpl service = new BookServiceImpl(mock(BookRepository.class), writer, guard);

    @Test
    void generatesInBatchesOf5000() {
        service.generateRandomBooks(12_345);

        verify(writer, times(3)).insert(anyList());
        assertThat(guard.isBusy()).isFalse();
    }

    @Test
    void generationFailsWhenABatchFails() {
        doThrow(new RuntimeException("db down")).when(writer).insert(anyList());

        assertThatThrownBy(() -> service.generateRandomBooks(10))
                .isInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("db down");
    }

    @Test
    void permitIsReleasedAfterFailure() {
        doThrow(new RuntimeException("db down")).when(writer).insert(anyList());
        assertThatThrownBy(() -> service.generateRandomBooks(10)).isInstanceOf(IllegalStateException.class);
        assertThat(guard.isBusy()).isFalse();

        reset(writer);
        service.generateRandomBooks(10);
        verify(writer).insert(anyList());
    }

    @Test
    void rejectsGenerationWhileAnotherBulkOperationRuns() {
        try (BulkOperationGuard.Permit _ = guard.acquire()) {
            assertThatThrownBy(() -> service.generateRandomBooks(10))
                    .isInstanceOf(BulkOperationInProgressException.class);
        }
    }

    @Test
    void rejectsOutOfRangeCount() {
        assertThatThrownBy(() -> service.generateRandomBooks(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.generateRandomBooks(BookServiceImpl.MAX_GENERATED_BOOKS + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
