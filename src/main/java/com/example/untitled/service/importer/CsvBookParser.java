package com.example.untitled.service.importer;

import com.example.untitled.entity.Book;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses CSV lines in the format {@code title,author,price}.
 * Supports quoted fields containing commas and escaped quotes ({@code ""}).
 */
final class CsvBookParser {

    private CsvBookParser() {
    }

    static boolean isBlank(String line) {
        return line == null || line.isBlank();
    }

    /**
     * @return the parsed book, or {@code null} if the line is malformed or has an invalid price
     */
    static Book parse(String line) {
        return BookFieldMapper.toBook(splitFields(line));
    }

    static List<String> splitFields(String line) {
        List<String> fields = new ArrayList<>(3);
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
