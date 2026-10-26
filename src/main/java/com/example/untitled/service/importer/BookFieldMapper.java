package com.example.untitled.service.importer;

import com.example.untitled.entity.Book;

import java.math.BigDecimal;
import java.util.List;

/**
 * Validates the {@code title, author, price} fields of one record and builds a {@link Book}.
 * Shared by the CSV and Excel readers so both formats follow the same rules.
 */
final class BookFieldMapper {

    private BookFieldMapper() {
    }

    /**
     * @return the book, or {@code null} if a field is missing, title/author is empty or the price is not a number
     */
    static Book toBook(List<String> fields) {
        if (fields.size() < 3) {
            return null;
        }
        String title = fields.get(0).trim();
        String author = fields.get(1).trim();
        if (title.isEmpty() || author.isEmpty()) {
            return null;
        }
        try {
            return new Book(title, author, new BigDecimal(fields.get(2).trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
