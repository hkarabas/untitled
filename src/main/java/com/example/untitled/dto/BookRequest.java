package com.example.untitled.dto;

import com.example.untitled.entity.Book;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request body for creating a book. Kept separate from the {@link Book} entity so a client cannot
 * set fields it does not own (such as {@code id}) through mass assignment.
 */
@Schema(description = "Data for a new book")
public record BookRequest(
        @NotBlank @Size(max = 255)
        @Schema(description = "Title of the book", example = "The Great Gatsby")
        String title,

        @NotBlank @Size(max = 255)
        @Schema(description = "Author of the book", example = "F. Scott Fitzgerald")
        String author,

        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2)
        @Schema(description = "Price of the book", example = "19.99")
        BigDecimal price) {

    public Book toBook() {
        return new Book(title, author, price);
    }
}
