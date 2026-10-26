package com.example.untitled.service.importer;

import com.example.untitled.service.BulkOperationGuard;
import com.example.untitled.service.BulkOperationInProgressException;
import com.example.untitled.service.ImportResult;
import com.example.untitled.service.impl.BookBatchWriter;
import com.example.untitled.support.XlsxTestFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Every strategy × every format through the facade, with real readers and a mocked database writer.
 */
class BookImportServiceTest {

    private static final List<String> STRATEGIES = List.of(
            TraditionalImportStrategy.NAME, VirtualThreadsImportStrategy.NAME, ParallelStreamImportStrategy.NAME);

    private final BookBatchWriter writer = mock(BookBatchWriter.class);
    private final BulkOperationGuard guard = new BulkOperationGuard();
    private final BookImportService service = new BookImportService(
            List.of(new TraditionalImportStrategy(writer),
                    new VirtualThreadsImportStrategy(writer),
                    new ParallelStreamImportStrategy(writer, 4)),
            new BookReaderFactory(), guard);

    // Same content in both formats: header, 2 valid rows, 1 blank row, 1 bad price, 1 valid row
    private static MockMultipartFile sample(ImportFormat format) {
        return switch (format) {
            case CSV -> csv("""
                    title,author,price
                    "Book A","Author A",10.00

                    "Book B","Author B",not-a-price
                    "Book C","Author C",12.50
                    """);
            case XLSX -> xlsx(XlsxTestFiles.xlsx(
                    new Object[]{"title", "author", "price"},
                    new Object[]{"Book A", "Author A", 10.00},
                    new Object[]{},
                    new Object[]{"Book B", "Author B", "not-a-price"},
                    new Object[]{"Book C", "Author C", 12.50}));
        };
    }

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "books.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile xlsx(byte[] content) {
        return new MockMultipartFile("file", "books.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
    }

    private static MockMultipartFile largeCsv(int rows) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            sb.append("\"Title ").append(i).append("\",\"Author\",9.99\n");
        }
        return csv(sb.toString());
    }

    private static MockMultipartFile largeXlsx(int rows) {
        return xlsx(XlsxTestFiles.xlsx(IntStream.range(0, rows)
                .mapToObj(i -> new Object[]{"Title " + i, "Author", 9.99})));
    }

    static Stream<Arguments> strategiesAndFormats() {
        return STRATEGIES.stream().flatMap(strategy ->
                Stream.of(ImportFormat.values()).map(format -> Arguments.of(strategy, format)));
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("strategiesAndFormats")
    void countsImportedAndSkippedRecords(String strategy, ImportFormat format) throws Exception {
        ImportResult result = service.importNow(sample(format), format, strategy);

        assertThat(result.imported()).isEqualTo(2);   // Book A, Book C
        assertThat(result.skipped()).isEqualTo(2);    // header + bad price; the blank row is ignored
        assertThat(guard.isBusy()).isFalse();
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("strategiesAndFormats")
    void failsWhenTheDatabaseFails(String strategy, ImportFormat format) {
        doThrow(new RuntimeException("db down")).when(writer).insert(anyList());

        // Some strategies wrap the error, others rethrow it as is: check the deepest cause
        assertThatThrownBy(() -> service.importNow(sample(format), format, strategy))
                .satisfies(e -> assertThat(deepestCause(e)).hasMessage("db down"));
        assertThat(guard.isBusy()).isFalse();
    }

    private static Throwable deepestCause(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    @Test
    void virtualThreadsStreamsAllBatchesIncludingPartialLastOne() throws Exception {
        ImportResult result = service.importNow(largeCsv(123_456), ImportFormat.CSV, VirtualThreadsImportStrategy.NAME);

        assertThat(result.imported()).isEqualTo(123_456);
        verify(writer, times(25)).insert(anyList()); // 24 full batches of 5000 + 1 of 3456
    }

    @Test
    void excelImportSpansMultipleBatches() throws Exception {
        ImportResult result = service.importNow(largeXlsx(12_345), ImportFormat.XLSX, VirtualThreadsImportStrategy.NAME);

        assertThat(result.imported()).isEqualTo(12_345);
        verify(writer, times(3)).insert(anyList());
    }

    @Test
    void virtualThreadsStopsReadingAfterFailure() {
        doThrow(new RuntimeException("db down")).when(writer).insert(anyList());

        assertThatThrownBy(() -> service.importNow(largeCsv(500_000), ImportFormat.CSV, VirtualThreadsImportStrategy.NAME))  // 100 batches
                .isInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("db down");
        verify(writer, atMost(25)).insert(anyList());
    }

    @Test
    void backgroundImportRunsAndCleansUp() throws Exception {
        long tempFilesBefore = countTempImportFiles();

        service.importInBackground(sample(ImportFormat.XLSX), ImportFormat.XLSX, VirtualThreadsImportStrategy.NAME);

        await().atMost(Duration.ofSeconds(10)).until(() -> !guard.isBusy());
        verify(writer).insert(anyList());
        assertThat(countTempImportFiles()).isEqualTo(tempFilesBefore);
    }

    @Test
    void backgroundFailureIsNotThrownToCallerAndReleasesPermit() throws Exception {
        doThrow(new RuntimeException("db down")).when(writer).insert(anyList());

        service.importInBackground(sample(ImportFormat.CSV), ImportFormat.CSV, TraditionalImportStrategy.NAME);

        await().atMost(Duration.ofSeconds(10)).until(() -> !guard.isBusy());
    }

    @Test
    void rejectsImportWhileAnotherBulkOperationRuns() {
        try (BulkOperationGuard.Permit _ = guard.acquire()) {
            assertThatThrownBy(() -> service.importNow(sample(ImportFormat.CSV), ImportFormat.CSV, TraditionalImportStrategy.NAME))
                    .isInstanceOf(BulkOperationInProgressException.class);
        }
    }

    @Test
    void rejectsUnknownStrategy() {
        assertThatThrownBy(() -> service.importNow(sample(ImportFormat.CSV), ImportFormat.CSV, "magic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("traditional");
    }

    @Test
    void rejectsUnsupportedOrEmptyFiles() {
        MockMultipartFile txt = new MockMultipartFile("file", "books.txt", "text/plain", "x".getBytes());
        MockMultipartFile xls = new MockMultipartFile("file", "books.xls", "application/vnd.ms-excel", "x".getBytes());
        MockMultipartFile empty = new MockMultipartFile("file", "books.csv", "text/csv", new byte[0]);

        for (MockMultipartFile file : List.of(txt, xls, empty)) {
            assertThatThrownBy(() -> service.importNow(file, ImportFormat.CSV, TraditionalImportStrategy.NAME))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(guard.isBusy()).isFalse();
    }

    @Test
    void eachEndpointAcceptsOnlyItsOwnFormat() {
        assertThatThrownBy(() -> service.importNow(sample(ImportFormat.XLSX), ImportFormat.CSV, TraditionalImportStrategy.NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only .csv files are allowed on this endpoint");
        assertThatThrownBy(() -> service.importNow(sample(ImportFormat.CSV), ImportFormat.XLSX, TraditionalImportStrategy.NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only .xlsx files are allowed on this endpoint");
        assertThat(guard.isBusy()).isFalse();
    }

    @Test
    void rejectsCorruptExcelFileAndCleansUp() throws IOException {
        long tempFilesBefore = countTempImportFiles();

        assertThatThrownBy(() -> service.importNow(xlsx("not a zip".getBytes()), ImportFormat.XLSX, TraditionalImportStrategy.NAME))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(guard.isBusy()).isFalse();
        assertThat(countTempImportFiles()).isEqualTo(tempFilesBefore);
    }

    private static long countTempImportFiles() throws IOException {
        try (Stream<Path> files = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return files.filter(p -> p.getFileName().toString().startsWith("books_import_")).count();
        }
    }
}
