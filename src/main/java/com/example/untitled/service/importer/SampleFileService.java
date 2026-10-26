package com.example.untitled.service.importer;

import com.example.untitled.entity.Book;
import com.example.untitled.service.RandomBooks;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Writes random book files in the import format (header row + {@code title, author, price}),
 * so they can be uploaded straight back to the import endpoints. Rows are streamed to the output:
 * memory use does not grow with the row count.
 */
@Service
public class SampleFileService {

    private static final Logger log = LoggerFactory.getLogger(SampleFileService.class);

    /** Excel allows 1,048,576 rows per sheet; this keeps both formats at the same limit. */
    public static final int MAX_ROWS = 1_000_000;

    /** Excel rows kept in memory while writing; older rows are flushed to a temp file. */
    private static final int EXCEL_ROW_WINDOW = 1000;

    public String filename(ImportFormat format, int rows) {
        return "books_random_" + rows + format.extension();
    }

    /**
     * Writes {@code rows} random books plus a header row. Does not close {@code out}.
     */
    public void write(ImportFormat format, int rows, OutputStream out) throws IOException {
        if (rows < 1 || rows > MAX_ROWS) {
            throw new IllegalArgumentException("rows must be between 1 and " + MAX_ROWS);
        }
        long start = System.currentTimeMillis();
        switch (format) {
            case CSV -> writeCsv(rows, out);
            case XLSX -> writeExcel(rows, out);
        }
        log.info("Sample file written: format={}, rows={}, durationMs={}", format, rows, System.currentTimeMillis() - start);
    }

    private static void writeCsv(int rows, OutputStream out) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), 64 * 1024);
        writer.write("title,author,price\n");
        for (int i = 1; i <= rows; i++) {
            Book book = RandomBooks.create(i);
            writer.write(quote(book.getTitle()));
            writer.write(',');
            writer.write(quote(book.getAuthor()));
            writer.write(',');
            writer.write(book.getPrice().toPlainString());
            writer.write('\n');
        }
        writer.flush(); // not close(): the caller owns the stream
    }

    private static String quote(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static void writeExcel(int rows, OutputStream out) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(EXCEL_ROW_WINDOW)) {
            workbook.setCompressTempFiles(true);
            Sheet sheet = workbook.createSheet("books");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("title");
            header.createCell(1).setCellValue("author");
            header.createCell(2).setCellValue("price");
            for (int i = 1; i <= rows; i++) {
                Book book = RandomBooks.create(i);
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue(book.getTitle());
                row.createCell(1).setCellValue(book.getAuthor());
                row.createCell(2).setCellValue(book.getPrice().doubleValue()); // numeric cell, like a real spreadsheet
            }
            workbook.write(out);
        }
    }
}
