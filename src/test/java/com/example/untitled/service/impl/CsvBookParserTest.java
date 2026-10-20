package com.example.untitled.service.impl;

import com.example.untitled.entity.Book;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvBookParserTest {

    @Test
    void parsesQuotedLine() {
        Book book = CsvBookParser.parse("\"The Infinite Quest 1\",\"Michael Martin\",84.97");

        assertThat(book).isNotNull();
        assertThat(book.getTitle()).isEqualTo("The Infinite Quest 1");
        assertThat(book.getAuthor()).isEqualTo("Michael Martin");
        assertThat(book.getPrice()).isEqualByComparingTo(new BigDecimal("84.97"));
    }

    @Test
    void parsesUnquotedLine() {
        Book book = CsvBookParser.parse("The Great Adventure,John Smith,29.99");

        assertThat(book).isNotNull();
        assertThat(book.getTitle()).isEqualTo("The Great Adventure");
    }

    @Test
    void handlesCommasAndEscapedQuotesInsideQuotes() {
        assertThat(CsvBookParser.splitFields("\"Hello, \"\"World\"\"\",Author,1.00"))
                .containsExactly("Hello, \"World\"", "Author", "1.00");
    }

    @Test
    void rejectsInvalidLines() {
        assertThat(CsvBookParser.parse("title,author,price")).isNull();   // header row
        assertThat(CsvBookParser.parse("only,two")).isNull();
        assertThat(CsvBookParser.parse(",Author,1.00")).isNull();
    }

    @Test
    void detectsBlankLines() {
        assertThat(List.of("", "   ")).allMatch(CsvBookParser::isBlank);
        assertThat(CsvBookParser.isBlank("a,b,1")).isFalse();
    }
}
