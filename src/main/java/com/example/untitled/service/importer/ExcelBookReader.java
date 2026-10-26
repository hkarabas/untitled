package com.example.untitled.service.importer;

import com.github.pjfanning.xlsx.StreamingReader;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Reads the first sheet of an .xlsx file row by row with a streaming parser,
 * so memory stays small even for a full 1,048,576-row sheet.
 * Columns A, B, C are title, author, price; other columns are ignored.
 */
final class ExcelBookReader implements BookReader {

    private static final int COLUMNS = 3;

    private final Workbook workbook;
    private final Iterator<Row> rows;
    private long rowNo;

    ExcelBookReader(Path file) {
        try {
            this.workbook = StreamingReader.builder()
                    .rowCacheSize(1000)      // rows kept in memory at a time
                    .bufferSize(64 * 1024)
                    .open(file.toFile());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Not a valid .xlsx file: " + e.getMessage(), e);
        }
        this.rows = workbook.getNumberOfSheets() == 0
                ? Collections.emptyIterator()
                : workbook.getSheetAt(0).rowIterator();
    }

    @Override
    public BookRecord next() {
        while (rows.hasNext()) {
            Row row = rows.next();
            rowNo = row.getRowNum() + 1L;
            List<String> fields = new ArrayList<>(COLUMNS);
            boolean blank = true;
            for (int column = 0; column < COLUMNS; column++) {
                String text = cellText(row.getCell(column));
                blank &= text.isBlank();
                fields.add(text);
            }
            if (!blank) {
                return () -> BookFieldMapper.toBook(fields);
            }
        }
        return null;
    }

    /**
     * Converts a cell to text. Numbers keep their exact decimal form without a trailing ".0"
     * (84.97 → "84.97", 1984 → "1984"); formulas use their cached result.
     */
    static String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    @Override
    public long position() {
        return rowNo;
    }

    @Override
    public void close() throws IOException {
        workbook.close();
    }
}
