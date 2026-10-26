package com.example.untitled.service;

import com.example.untitled.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * CRUD and random generation. File imports live in {@link com.example.untitled.service.importer.BookImportService}.
 */
public interface BookService {
    Page<Book> findPage(Pageable pageable);
    Optional<Book> findById(Long id);
    Book save(Book book);

    /**
     * @return {@code true} if the book existed and was deleted
     */
    boolean deleteById(Long id);

    void generateRandomBooks(int count);
}
