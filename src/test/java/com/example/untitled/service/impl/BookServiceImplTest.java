package com.example.untitled.service.impl;

import com.example.untitled.repository.BookRepository;
import com.example.untitled.service.ImportResult;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BookServiceImplTest {

    private final BookBatchWriter writer = mock(BookBatchWriter.class);
    private final BookServiceImpl service = new BookServiceImpl(mock(BookRepository.class), writer, 4);

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "books.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void countsImportedAndSkippedLines() throws Exception {
        MockMultipartFile file = csv("""
                title,author,price
                "Book A","Author A",10.00

                "Book B","Author B",not-a-price
                "Book C","Author C",12.50
                """);

        for (ImportResult result : List.of(
                service.importCsvTraditional(file),
                service.importCsvVirtualThreads(file),
                service.importCsvParallelStream(file))) {
            assertThat(result.imported()).isEqualTo(2);
            assertThat(result.skipped()).isEqualTo(2);
        }
    }

    @Test
    void virtualThreadImportFailsWhenABatchFails() {
        doThrow(new RuntimeException("db down")).when(writer).insert(anyList());

        assertThatThrownBy(() -> service.importCsvVirtualThreads(csv("\"A\",\"B\",1.00\n")))
                .isInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("db down");
    }

    private static MockMultipartFile largeCsv(int lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines; i++) {
            sb.append("\"Title ").append(i).append("\",\"Author\",9.99\n");
        }
        return csv(sb.toString());
    }

    @Test
    void virtualThreadImportStreamsAllBatchesIncludingPartialLastOne() throws Exception {
        ImportResult result = service.importCsvVirtualThreads(largeCsv(123_456));

        assertThat(result.imported()).isEqualTo(123_456);
        assertThat(result.skipped()).isZero();
        verify(writer, times(25)).insert(anyList()); // 24 full batches of 5000 + 1 of 3456
    }

    @Test
    void virtualThreadImportStopsReadingAfterFailure() {
        doThrow(new RuntimeException("db down")).when(writer).insert(anyList());

        assertThatThrownBy(() -> service.importCsvVirtualThreads(largeCsv(500_000)))  // 100 batches
                .isInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("db down");
        // Only batches already in flight when the first failure happened may have been attempted
        verify(writer, atMost(25)).insert(anyList());
    }

    @Test
    void generationFailsWhenABatchFails() {
        doThrow(new RuntimeException("db down")).when(writer).insert(anyList());

        assertThatThrownBy(() -> service.generateRandomBooks(10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void permitIsReleasedAfterFailure() throws Exception {
        doThrow(new RuntimeException("db down")).when(writer).insert(anyList());
        assertThatThrownBy(() -> service.generateRandomBooks(10)).isInstanceOf(IllegalStateException.class);

        reset(writer);
        assertThat(service.importCsvTraditional(csv("\"A\",\"B\",1.00\n")).imported()).isEqualTo(1);
    }

    @Test
    void rejectsOutOfRangeCount() {
        assertThatThrownBy(() -> service.generateRandomBooks(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.generateRandomBooks(BookServiceImpl.MAX_GENERATED_BOOKS + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonCsvUpload() {
        MockMultipartFile file = new MockMultipartFile("file", "books.txt", "text/plain", "x".getBytes());
        assertThatThrownBy(() -> service.importCsvTraditional(file)).isInstanceOf(IllegalArgumentException.class);
    }
}
