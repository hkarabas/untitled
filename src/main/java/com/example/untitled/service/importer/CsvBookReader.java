package com.example.untitled.service.importer;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Reads a CSV file line by line. Lines are parsed lazily in {@link BookRecord#toBook()},
 * so the splitting work can run on the strategy's worker threads.
 */
final class CsvBookReader implements BookReader {

    private final BufferedReader reader;
    private long lineNo;

    CsvBookReader(BufferedReader reader) {
        this.reader = reader;
    }

    @Override
    public BookRecord next() throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            lineNo++;
            if (!CsvBookParser.isBlank(line)) {
                String csvLine = line;
                return () -> CsvBookParser.parse(csvLine);
            }
        }
        return null;
    }

    @Override
    public long position() {
        return lineNo;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
