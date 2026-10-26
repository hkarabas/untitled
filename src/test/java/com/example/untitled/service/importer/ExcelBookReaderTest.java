package com.example.untitled.service.importer;

import com.example.untitled.entity.Book;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcelBookReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsCellTypesBlankRowsAndFormulas() throws Exception {
        Path file = tempDir.resolve("books.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file)) {
            XSSFSheet sheet = workbook.createSheet("books");
            text(sheet.createRow(0), "title", "author", "price");                       // header → invalid
            Row numericPrice = sheet.createRow(1);
            text(numericPrice, "The Infinite Quest 1", "Michael Martin");
            numericPrice.createCell(2).setCellValue(84.97);
            text(sheet.createRow(2), "Text Price", "Someone", "12.50");                 // price stored as text
            sheet.createRow(3);                                                        // blank row → skipped
            Row numericTitle = sheet.createRow(4);
            numericTitle.createCell(0).setCellValue(1984);                             // numeric title
            numericTitle.createCell(1).setCellValue("George Orwell");
            numericTitle.createCell(2).setCellValue(9);
            Row formula = sheet.createRow(5);
            text(formula, "Formula Price", "Calc");
            formula.createCell(2).setCellFormula("10+5.5");
            text(sheet.createRow(6), "No Author", "", "5.00");                           // missing author → invalid
            Row extra = sheet.createRow(7);
            text(extra, "Extra Columns", "Author X", "7.25", "ignored");
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            workbook.write(out);
        }

        List<Book> books = new ArrayList<>();
        List<Long> positions = new ArrayList<>();
        int invalid = 0;
        try (ExcelBookReader reader = new ExcelBookReader(file)) {
            BookRecord record;
            while ((record = reader.next()) != null) {
                positions.add(reader.position());
                Book book = record.toBook();
                if (book == null) {
                    invalid++;
                } else {
                    books.add(book);
                }
            }
        }

        assertThat(positions).containsExactly(1L, 2L, 3L, 5L, 6L, 7L, 8L);   // row 4 (blank) never returned
        assertThat(invalid).isEqualTo(2);
        assertThat(books).extracting(Book::getTitle)
                .containsExactly("The Infinite Quest 1", "Text Price", "1984", "Formula Price", "Extra Columns");
        assertThat(books).extracting(Book::getPrice).usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("84.97"), new BigDecimal("12.50"), new BigDecimal("9"),
                        new BigDecimal("15.5"), new BigDecimal("7.25"));
    }

    @Test
    void rejectsFileThatIsNotXlsx() throws Exception {
        Path file = tempDir.resolve("fake.xlsx");
        Files.writeString(file, "title,author,price\nA,B,1.00\n");

        assertThatThrownBy(() -> new ExcelBookReader(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Not a valid .xlsx file");
    }

    private static void text(Row row, String... values) {
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }
}
