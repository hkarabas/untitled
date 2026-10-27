package com.example.untitled.controller;

import com.example.untitled.service.importer.BookImportService;
import com.example.untitled.service.importer.ImportFormat;
import com.example.untitled.service.importer.ParallelStreamImportStrategy;
import com.example.untitled.service.importer.SampleFileService;
import com.example.untitled.service.importer.TraditionalImportStrategy;
import com.example.untitled.service.importer.VirtualThreadsImportStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;

/**
 * CSV import endpoints (accept only {@code .csv} files) and a random CSV sample-file download.
 */
@RestController
@RequestMapping("/api/books")
@Tag(name = "CSV Import", description = "Import books from .csv files, or download a random .csv file")
public class CsvImportController extends AbstractImportController {

    public CsvImportController(BookImportService importService, SampleFileService sampleFiles) {
        super(importService, sampleFiles, ImportFormat.CSV);
    }

    @GetMapping("/sample/csv")
    @Operation(summary = "Download a random CSV file",
            description = "Generates a .csv file with a header row and 'rows' random books (quoted title and author, price), "
                    + "ready to upload to the CSV import endpoints. Max 1,000,000 rows.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV file download", content = @Content(mediaType = "text/csv")),
            @ApiResponse(responseCode = "400", description = "rows out of range")
    })
    public ResponseEntity<StreamingResponseBody> sampleCsv(
            @RequestParam(defaultValue = "100000") @Min(1) @Max(SampleFileService.MAX_ROWS)
            @Parameter(description = "Number of random books", example = "100000") int rows) {
        return sampleFile(rows);
    }

    @PostMapping(value = "/import/csv", consumes = "multipart/form-data")
    @Operation(summary = "Import books from a CSV file",
            description = "Upload a .csv file (UTF-8) with columns title, author, price. "
                    + "Choose how it is processed with 'strategy'. With async=true the request returns 202 immediately "
                    + "and the import runs in the background (outcome only logged).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File imported successfully"),
            @ApiResponse(responseCode = "202", description = "Import accepted and running in the background (async=true)"),
            @ApiResponse(responseCode = "400", description = "Not a .csv file, or unknown strategy"),
            @ApiResponse(responseCode = "429", description = "Another bulk operation is running")
    })
    public ResponseEntity<String> importCsv(
            @RequestParam("file") @Parameter(description = ".csv file to upload", required = true) MultipartFile file,
            @RequestParam(defaultValue = VirtualThreadsImportStrategy.STRATEGY_NAME)
            @Parameter(description = "How to process the file", schema = @Schema(allowableValues = {
                    TraditionalImportStrategy.STRATEGY_NAME, VirtualThreadsImportStrategy.STRATEGY_NAME, ParallelStreamImportStrategy.STRATEGY_NAME}))
            String strategy,
            @RequestParam(defaultValue = "false") @Parameter(description = "Run in the background and return 202 immediately")
            boolean async) throws IOException {
        return runImport(file, strategy, async);
    }

    // Original CSV endpoints, kept for existing scripts and the Postman collection.
    // Shortcuts for /import/csv with a fixed strategy.

    @PostMapping(value = "/import-csv-traditional", consumes = "multipart/form-data")
    @Operation(summary = "Import books from CSV (Traditional)", description = "Same as /import/csv?strategy=traditional")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV imported successfully"),
            @ApiResponse(responseCode = "400", description = "Not a .csv file"),
            @ApiResponse(responseCode = "429", description = "Another bulk operation is running")
    })
    public ResponseEntity<String> importCsvTraditional(
            @RequestParam("file") @Parameter(description = ".csv file to upload", required = true) MultipartFile file) throws IOException {
        return runImport(file, TraditionalImportStrategy.STRATEGY_NAME, false);
    }

    @PostMapping(value = "/import-csv-virtual-threads", consumes = "multipart/form-data")
    @Operation(summary = "Import books from CSV (Virtual Threads)", description = "Same as /import/csv?strategy=virtual-threads")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV imported successfully"),
            @ApiResponse(responseCode = "400", description = "Not a .csv file"),
            @ApiResponse(responseCode = "429", description = "Another bulk operation is running")
    })
    public ResponseEntity<String> importCsvVirtualThreads(
            @RequestParam("file") @Parameter(description = ".csv file to upload", required = true) MultipartFile file) throws IOException {
        return runImport(file, VirtualThreadsImportStrategy.STRATEGY_NAME, false);
    }

    @PostMapping(value = "/import-csv-parallel-fire-and-forget", consumes = "multipart/form-data")
    @Operation(summary = "Import books from CSV (Fire-and-Forget)", description = "Same as /import/csv?strategy=virtual-threads&async=true. Returns immediately; the outcome is only logged.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "CSV import accepted and processing in background"),
            @ApiResponse(responseCode = "400", description = "Not a .csv file"),
            @ApiResponse(responseCode = "429", description = "Another bulk operation is running")
    })
    public ResponseEntity<String> importCsvParallelFireAndForget(
            @RequestParam("file") @Parameter(description = ".csv file to upload", required = true) MultipartFile file) throws IOException {
        return runImport(file, VirtualThreadsImportStrategy.STRATEGY_NAME, true);
    }

    @PostMapping(value = "/import-csv-parallel-stream", consumes = "multipart/form-data")
    @Operation(summary = "Import books from CSV (Parallel Streams)", description = "Same as /import/csv?strategy=parallel-stream")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV imported successfully"),
            @ApiResponse(responseCode = "400", description = "Not a .csv file"),
            @ApiResponse(responseCode = "429", description = "Another bulk operation is running")
    })
    public ResponseEntity<String> importCsvParallelStream(
            @RequestParam("file") @Parameter(description = ".csv file to upload", required = true) MultipartFile file) throws IOException {
        return runImport(file, ParallelStreamImportStrategy.STRATEGY_NAME, false);
    }
}
