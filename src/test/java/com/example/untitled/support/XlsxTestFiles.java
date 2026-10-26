package com.example.untitled.support;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

/**
 * Builds .xlsx files in memory for tests. Uses POI's streaming writer, so large sheets are cheap.
 */
public final class XlsxTestFiles {

    private XlsxTestFiles() {
    }

    /**
     * One row per array. A {@link Number} becomes a numeric cell, anything else a text cell, {@code null} no cell.
     */
    public static byte[] xlsx(Stream<Object[]> rows) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("books");
            int[] rowNo = {0};
            rows.forEach(values -> {
                Row row = sheet.createRow(rowNo[0]++);
                for (int column = 0; column < values.length; column++) {
                    Object value = values[column];
                    if (value == null) {
                        continue;
                    }
                    Cell cell = row.createCell(column);
                    if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
            });
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] xlsx(Object[]... rows) {
        return xlsx(Stream.of(rows));
    }
}
