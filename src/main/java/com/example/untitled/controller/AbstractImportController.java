package com.example.untitled.controller;

import com.example.untitled.service.ImportResult;
import com.example.untitled.service.importer.BookImportService;
import com.example.untitled.service.importer.ImportFormat;
import com.example.untitled.service.importer.SampleFileService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;

/**
 * Shared request handling for the per-format import controllers: each subclass fixes the {@link ImportFormat}
 * it accepts, so a CSV endpoint never reads an Excel file and vice versa.
 */
abstract class AbstractImportController {

    private final BookImportService importService;
    private final SampleFileService sampleFiles;
    private final ImportFormat format;

    protected AbstractImportController(BookImportService importService, SampleFileService sampleFiles, ImportFormat format) {
        this.importService = importService;
        this.sampleFiles = sampleFiles;
        this.format = format;
    }

    protected ResponseEntity<String> runImport(MultipartFile file, String strategy, boolean async) throws IOException {
        if (async) {
            importService.importInBackground(file, format, strategy);
            return ResponseEntity.accepted().body(String.format(
                    "%s import accepted. Processing started in background (strategy: %s).", format, strategy));
        }
        ImportResult result = importService.importNow(file, format, strategy);
        return ResponseEntity.ok(String.format(
                "%s import completed successfully (strategy: %s): %d books imported, %d invalid records skipped, in %d ms (%.2f seconds)",
                format, strategy, result.imported(), result.skipped(), result.durationMs(), result.durationMs() / 1000.0));
    }

    /**
     * Streams a random sample file as a download. Generation runs after the headers are sent,
     * so even a 1M-row file needs little memory.
     */
    protected ResponseEntity<StreamingResponseBody> sampleFile(int rows) {
        String filename = sampleFiles.filename(format, rows);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(format.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(out -> sampleFiles.write(format, rows, out));
    }
}
