package com.example.untitled.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringBootVersion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Books API");
        response.put("version", "1.0.0");
        response.put("description", "REST API for managing a library of books");
        response.put("endpoints", Map.of(
                "API Docs", "/swagger-ui/index.html",
                "OpenAPI Spec", "/v3/api-docs",
                "API Base", "/api/books"
        ));
        response.put("quick_links", Map.of(
                "Get all books", "GET /api/books",
                "Get book by ID", "GET /api/books/{id}",
                "Create book", "POST /api/books",
                "Delete book", "DELETE /api/books/{id}",
                "Generate random books", "POST /api/books/generate-random",
                "Import CSV (traditional)", "POST /api/books/import-csv-traditional",
                "Import CSV (virtual threads)", "POST /api/books/import-csv-virtual-threads",
                "Import CSV (parallel fire-and-forget)", "POST /api/books/import-csv-parallel-fire-and-forget",
                "Import CSV (parallel streams)", "POST /api/books/import-csv-parallel-stream"
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Books API is running");
        response.put("timestamp", System.currentTimeMillis() + "");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "Books REST API");
        response.put("version", "1.0.0");
        response.put("description", "REST API for managing a library of books with support for CRUD operations and bulk CSV imports");
        response.put("documentation", "/swagger-ui/index.html");
        response.put("openapi_spec", "/v3/api-docs");
        response.put("java_version", System.getProperty("java.version"));
        response.put("framework", "Spring Boot " + SpringBootVersion.getVersion());
        return ResponseEntity.ok(response);
    }
}
