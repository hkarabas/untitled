package com.example.untitled.service.importer;

import java.util.Locale;

/**
 * Supported upload formats, detected from the file extension.
 * Both use the same columns: {@code title, author, price}.
 */
public enum ImportFormat {
    CSV(".csv", "text/csv"),
    XLSX(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final String extension;
    private final String mediaType;

    ImportFormat(String extension, String mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String extension() {
        return extension;
    }

    /** HTTP content type of files in this format. */
    public String mediaType() {
        return mediaType;
    }

    /**
     * @throws IllegalArgumentException if the filename does not end with this format's extension
     */
    public void requireFilename(String filename) {
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(extension)) {
            throw new IllegalArgumentException("Only " + extension + " files are allowed on this endpoint");
        }
    }
}
