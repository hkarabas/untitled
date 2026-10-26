package com.example.untitled.service.importer;

import com.example.untitled.entity.Book;

/**
 * One non-blank input record (a CSV line or an Excel row), not yet validated.
 * Conversion is deferred so strategies can run it on worker threads.
 */
@FunctionalInterface
public interface BookRecord {

    /**
     * @return the book, or {@code null} if the record is invalid (header row, bad price, missing field)
     */
    Book toBook();
}
