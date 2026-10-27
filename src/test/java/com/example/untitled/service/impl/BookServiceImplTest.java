package com.example.untitled.service.impl;

import com.example.untitled.entity.Book;
import com.example.untitled.repository.BookRepository;
import com.example.untitled.service.BulkOperationGuard;
import com.example.untitled.service.BulkOperationInProgressException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookServiceImplTest {

    private final BookBatchWriter writer = mock(BookBatchWriter.class);
    private final BulkOperationGuard guard = new BulkOperationGuard();
    private final BookRepository repository = mock(BookRepository.class);
    private final BookServiceImpl service = new BookServiceImpl(repository, writer, guard);

    @Test
    void crudDelegatesToRepository() {
        Book book = new Book("Title", "Author");
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> page = new PageImpl<>(List.of(book));
        when(repository.findAll(pageable)).thenReturn(page);
        when(repository.findById(1L)).thenReturn(Optional.of(book));
        when(repository.save(book)).thenReturn(book);

        assertThat(service.findPage(pageable)).isSameAs(page);
        assertThat(service.findById(1L)).contains(book);
        assertThat(service.save(book)).isSameAs(book);
    }

    @Test
    void deleteReturnsWhetherTheBookExisted() {
        when(repository.existsById(1L)).thenReturn(true);

        assertThat(service.deleteById(1L)).isTrue();
        verify(repository).deleteById(1L);

        assertThat(service.deleteById(2L)).isFalse();
        verify(repository, never()).deleteById(2L);
    }

    @Test
    void generationFailsWhenInterruptedWhileWaiting() {
        CountDownLatch neverReleased = new CountDownLatch(1);
        doAnswer(inv -> {
            neverReleased.await(); // ends when shutdownNow() interrupts the writer
            return null;
        }).when(writer).insert(anyList());

        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> service.generateRandomBooks(10))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Bulk operation interrupted");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted(); // clear the flag for the next tests
        }
        assertThat(guard.isBusy()).isFalse();
    }

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
