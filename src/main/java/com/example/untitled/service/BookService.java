package com.example.untitled.service;

import com.example.untitled.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

public interface BookService {
    Page<Book> findPage(Pageable pageable);
    Optional<Book> findById(Long id);
    Book save(Book book);

    /**
     * @return {@code true} if the book existed and was deleted
     */
    boolean deleteById(Long id);

    void generateRandomBooks(int count);
    ImportResult importCsvTraditional(MultipartFile file) throws IOException;
    ImportResult importCsvVirtualThreads(MultipartFile file) throws IOException;
    void importCsvParallelFireAndForget(MultipartFile file) throws IOException;
    ImportResult importCsvParallelStream(MultipartFile file) throws IOException;
}
