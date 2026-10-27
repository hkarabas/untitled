package com.example.untitled.service.importer;

import com.example.untitled.entity.Book;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SampleFileServiceTest {

    private final SampleFileService service = new SampleFileService();

    @TempDir
    Path tempDir;

    /** Round trip: every generated row must be readable by the import readers. */
    @ParameterizedTest
    @EnumSource(ImportFormat.class)
    void generatedFileCanBeImported(ImportFormat format) throws Exception {
        int rows = 12_345;
        Path file = tempDir.resolve(service.filename(format, rows));
        try (OutputStream out = Files.newOutputStream(file)) {
            service.write(format, rows, out);
        }

        int invalid = 0;
        Set<String> titles = new HashSet<>();
        try (BookReader reader = new BookReaderFactory().open(format, file)) {
            BookRecord record;
            while ((record = reader.next()) != null) {
                Book book = record.toBook();
                if (book == null) {
                    invalid++;
                    continue;
                }
                titles.add(book.getTitle());
                assertThat(book.getAuthor()).isNotBlank();
                assertThat(book.getPrice()).isBetween(new BigDecimal("5.99"), new BigDecimal("99.99"));
            }
        }

        assertThat(invalid).isEqualTo(1);          // the header row
        assertThat(titles).hasSize(rows);          // every title is unique
        // Titles look like "The Hidden Quest 42" and end with the row numbers 1..rows
        assertThat(titles).allMatch(t -> t.matches("The \\w+ \\w+ \\d+"));
        assertThat(titles.stream().map(t -> Integer.parseInt(t.substring(t.lastIndexOf(' ') + 1))))
                .containsExactlyInAnyOrderElementsOf(java.util.stream.IntStream.rangeClosed(1, rows).boxed().toList());
    }

    @Test
    void filenameContainsRowsAndExtension() {
        assertThat(service.filename(ImportFormat.XLSX, 500)).isEqualTo("books_random_500.xlsx");
        assertThat(service.filename(ImportFormat.CSV, 500)).isEqualTo("books_random_500.csv");
    }

    @Test
    void rejectsOutOfRangeRowCount() {
        assertThatThrownBy(() -> service.write(ImportFormat.XLSX, 0, OutputStream.nullOutputStream()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.write(ImportFormat.CSV, SampleFileService.MAX_ROWS + 1, OutputStream.nullOutputStream()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingFormat() {
        assertThatThrownBy(() -> service.write(null, 1, OutputStream.nullOutputStream()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("format is required");
    }
}
