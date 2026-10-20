package com.example.untitled.controller;

import com.example.untitled.service.BulkOperationInProgressException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API errors.
 * Anything not handled here falls through to Spring's defaults (validation errors → 400, others → 500).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final DataSize maxFileSize;

    public GlobalExceptionHandler(@Value("${spring.servlet.multipart.max-file-size:200MB}") DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    /**
     * Handle MaxUploadSizeExceededException for file uploads exceeding configured limits.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {
        Map<String, Object> response = error(HttpStatus.CONTENT_TOO_LARGE, "File too large",
                "The uploaded file exceeds the maximum allowed size of " + maxFileSize.toMegabytes() + "MB");
        response.put("max_file_size_mb", maxFileSize.toMegabytes());
        return new ResponseEntity<>(response, HttpStatus.CONTENT_TOO_LARGE);
    }

    /**
     * Invalid input detected by the service layer (e.g. empty or non-CSV upload).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return new ResponseEntity<>(error(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BulkOperationInProgressException.class)
    public ResponseEntity<Map<String, Object>> handleBulkOperationInProgress(BulkOperationInProgressException ex) {
        return new ResponseEntity<>(error(HttpStatus.TOO_MANY_REQUESTS, "Busy", ex.getMessage()), HttpStatus.TOO_MANY_REQUESTS);
    }

    private static Map<String, Object> error(HttpStatus status, String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", error);
        response.put("message", message);
        response.put("status", status.value());
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
