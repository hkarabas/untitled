package com.example.untitled.service.impl;

import com.example.untitled.entity.Book;

import java.math.BigDecimal;
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
        List<String> fields = splitFields(line);
        if (fields.size() < 3) {
            return null;
        }
        String title = fields.get(0).trim();
        String author = fields.get(1).trim();
        if (title.isEmpty() || author.isEmpty()) {
            return null;
        }
        try {
            BigDecimal price = new BigDecimal(fields.get(2).trim());
            return new Book(title, author, price);
        } catch (NumberFormatException e) {
            return null;
        }
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
