package com.example.untitled.service.impl;

import com.example.untitled.entity.Book;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Inserts books in JDBC batches, one transaction per call.
 * <p>
 * Lives in its own bean so that {@code @Transactional} goes through the Spring proxy.
 * Uses JDBC directly because Hibernate cannot batch inserts for {@code IDENTITY} ids;
 * combined with {@code rewriteBatchedStatements=true} this becomes multi-row INSERTs.
 */
@Component
class BookBatchWriter {

    private static final String INSERT_SQL = "INSERT INTO books (title, author, price) VALUES (?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    BookBatchWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void insert(List<Book> books) {
        if (books.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(INSERT_SQL, books, books.size(), (ps, book) -> {
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setBigDecimal(3, book.getPrice());
        });
    }
}
