package com.example.untitled.service.impl;

import com.example.untitled.entity.Book;
import com.example.untitled.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(BookBatchWriter.class)
class BookBatchWriterTest {

    @Autowired
    private BookBatchWriter writer;

    @Autowired
    private BookRepository repository;

    @Test
    void insertsBooksIntoHibernateManagedTable() {
        writer.insert(List.of(
                new Book("Title 1", "Author 1", new BigDecimal("10.50")),
                new Book("Title 2", "Author 2", new BigDecimal("99.99"))));

        assertThat(repository.findAll())
                .extracting(Book::getTitle, Book::getAuthor, Book::getPrice)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Title 1", "Author 1", new BigDecimal("10.50")),
                        org.assertj.core.groups.Tuple.tuple("Title 2", "Author 2", new BigDecimal("99.99")));
    }
}
