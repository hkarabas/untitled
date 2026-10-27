package com.example.untitled.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BookTest {

    @Test
    void priceDefaultsToZero() {
        Book book = new Book("Title", "Author");

        assertThat(book.getId()).isNull();
        assertThat(book.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void settersUpdateFields() {
        Book book = new Book();
        book.setTitle("Title");
        book.setAuthor("Author");
        book.setPrice(new BigDecimal("9.99"));

        assertThat(book.getTitle()).isEqualTo("Title");
        assertThat(book.getAuthor()).isEqualTo("Author");
        assertThat(book.getPrice()).isEqualByComparingTo("9.99");
    }
}
