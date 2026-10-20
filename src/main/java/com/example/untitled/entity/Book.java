package com.example.untitled.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Entity
@Table(name = "books")
@Schema(description = "Book entity representing a book in the library")
public class Book {

    // Bulk inserts bypass Hibernate (see BookBatchWriter), so IDENTITY does not limit batching there.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier for the book", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    @Schema(description = "Title of the book", example = "The Great Gatsby")
    private String title;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    @Schema(description = "Author of the book", example = "F. Scott Fitzgerald")
    private String author;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    @Column(nullable = false, precision = 10, scale = 2)
    @Schema(description = "Price of the book", example = "19.99")
    private BigDecimal price;

    public Book() {
        this.price = BigDecimal.ZERO;
    }

    public Book(String title, String author) {
        this(title, author, BigDecimal.ZERO);
    }

    public Book(String title, String author, BigDecimal price) {
        this.title = title;
        this.author = author;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
