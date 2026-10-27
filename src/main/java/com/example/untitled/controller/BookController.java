package com.example.untitled.controller;

import com.example.untitled.dto.BookRequest;
import com.example.untitled.entity.Book;
import com.example.untitled.service.BookService;
import com.example.untitled.service.impl.BookServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/books")
@Tag(name = "Books", description = "API for managing books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    @Operation(summary = "Get books page", description = "Retrieve one page of books (100 books per page, zero-based page index)")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of books")
    public Page<Book> all(
            @RequestParam(defaultValue = "0") @Min(0) @Parameter(description = "Zero-based page index", example = "0") int page) {
        return bookService.findPage(PageRequest.of(page, 100, Sort.by(Sort.Direction.ASC, "id")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID", description = "Retrieve a specific book by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book found"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<Book> byId(@PathVariable @Parameter(description = "Book ID") Long id) {
        return bookService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new book", description = "Add a new book to the database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Book created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid book data")
    })
    public ResponseEntity<Book> create(@Valid @RequestBody BookRequest request) {
        Book saved = bookService.save(request.toBook());
        return ResponseEntity.created(URI.create("/api/books/" + saved.getId())).body(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a book", description = "Remove a book from the database by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Book deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<Void> delete(@PathVariable @Parameter(description = "Book ID") Long id) {
        return bookService.deleteById(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/generate-random")
    @Operation(summary = "Generate random books", description = "Generate a specified number of random books in the database (max 1,000,000)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Books generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid count"),
            @ApiResponse(responseCode = "429", description = "Another bulk operation is running")
    })
    public ResponseEntity<String> generateRandomBooks(
            @RequestParam(defaultValue = "100000") @Min(1) @Max(BookServiceImpl.MAX_GENERATED_BOOKS)
            @Parameter(description = "Number of books to generate", example = "100000") int count) {
        long startTime = System.currentTimeMillis();
        bookService.generateRandomBooks(count);
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(String.format("Successfully generated %d random books in %d ms (%.2f seconds)",
                count, duration, duration / 1000.0));
    }
}
