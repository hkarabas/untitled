package com.example.untitled.service;

/**
 * Thrown when a bulk operation (CSV import or random generation) is requested
 * while another one is still running.
 */
public class BulkOperationInProgressException extends RuntimeException {

    public BulkOperationInProgressException() {
        super("Another bulk import or generation is already running. Try again later.");
    }
}
