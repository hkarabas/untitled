package com.example.untitled.service;

/**
 * Outcome of a synchronous CSV import.
 *
 * @param imported   rows inserted into the database
 * @param skipped    non-blank rows that could not be parsed
 * @param durationMs wall-clock time of the import
 */
public record ImportResult(long imported, long skipped, long durationMs) {
}
