package com.example.untitled.service.importer;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Factory pattern: creates the right {@link BookReader} for a file format.
 * Adding a new format means adding an {@link ImportFormat} constant and one case here;
 * no import strategy has to change.
 */
@Component
public class BookReaderFactory {

    public BookReader open(ImportFormat format, Path file) throws IOException {
        return switch (format) {
            case CSV -> new CsvBookReader(Files.newBufferedReader(file, StandardCharsets.UTF_8));
            case XLSX -> new ExcelBookReader(file);
        };
    }
}
