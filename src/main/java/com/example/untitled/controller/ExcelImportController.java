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
 * Excel import endpoints (accept only {@code .xlsx} files) and a random Excel sample-file download.
 */
@RestController
@RequestMapping("/api/books")
@Tag(name = "Excel Import", description = "Import books from .xlsx files, or download a random .xlsx file")
public class ExcelImportController extends AbstractImportController {

    public ExcelImportController(BookImportService importService, SampleFileService sampleFiles) {
        super(importService, sampleFiles, ImportFormat.XLSX);
    }

    @GetMapping("/sample/excel")
    @Operation(summary = "Download a random Excel file",
            description = "Generates an .xlsx file with a header row and 'rows' random books (title, author, numeric price), "
                    + "ready to upload to the Excel import endpoints. Max 1,000,000 rows.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Excel file download",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "400", description = "rows out of range")
    })
    public ResponseEntity<StreamingResponseBody> sampleExcel(
            @RequestParam(defaultValue = "100000") @Min(1) @Max(SampleFileService.MAX_ROWS)
            @Parameter(description = "Number of random books", example = "100000") int rows) {
        return sampleFile(rows);
    }

    @PostMapping(value = "/import/excel", consumes = "multipart/form-data")
    @Operation(summary = "Import books from an Excel file",
            description = "Upload an .xlsx file; the first sheet is read, columns A, B, C = title, author, price. "
                    + "Old binary .xls files are not supported. "
                    + "Choose how it is processed with 'strategy'. With async=true the request returns 202 immediately "
                    + "and the import runs in the background (outcome only logged).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File imported successfully"),
            @ApiResponse(responseCode = "202", description = "Import accepted and running in the background (async=true)"),
            @ApiResponse(responseCode = "400", description = "Not a valid .xlsx file, or unknown strategy"),
            @ApiResponse(responseCode = "429", description = "Another bulk operation is running")
    })
    public ResponseEntity<String> importExcel(
            @RequestParam("file") @Parameter(description = ".xlsx file to upload", required = true) MultipartFile file,
            @RequestParam(defaultValue = VirtualThreadsImportStrategy.STRATEGY_NAME)
            @Parameter(description = "How to process the file", schema = @Schema(allowableValues = {
                    TraditionalImportStrategy.STRATEGY_NAME, VirtualThreadsImportStrategy.STRATEGY_NAME, ParallelStreamImportStrategy.STRATEGY_NAME}))
            String strategy,
            @RequestParam(defaultValue = "false") @Parameter(description = "Run in the background and return 202 immediately")
            boolean async) throws IOException {
        return runImport(file, strategy, async);
    }

    // Shortcuts for /import/excel with a fixed strategy, matching the /import-csv-* endpoints.

    @PostMapping(value = "/import-excel-traditional", consumes = "multipart/form-data")
    @Operation(summary = "Import books from Excel (Traditional)", description = "Same as /import/excel?strategy=traditional")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Excel file imported successfully"),
            @ApiResponse(responseCode = "400", description = "Not a valid .xlsx file"),
            @ApiResponse(responseCode = "429", description = "Another bulk operation is running")
    })
    public ResponseEntity<String> importExcelTraditional(
            @RequestParam("file") @Parameter(description = ".xlsx file to upload", required = true) MultipartFile file) throws IOException {
        return runImport(file, TraditionalImportStrategy.STRATEGY_NAME, false);
    }

    @PostMapping(value = "/import-excel-virtual-threads", consumes = "multipart/form-data")
    @Operation(summary = "Import books from Excel (Virtual Threads)", description = "Same as /import/excel?strategy=virtual-threads")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Excel file imported successfully"),
            @ApiResponse(responseCode = "400", description = "Not a valid .xlsx file"),
            @ApiResponse(responseCode = "429", description = "Another bulk operation is running")
    })
    public ResponseEntity<String> importExcelVirtualThreads(
            @RequestParam("file") @Parameter(description = ".xlsx file to upload", required = true) MultipartFile file) throws IOException {
        return runImport(file, VirtualThreadsImportStrategy.STRATEGY_NAME, false);
    }

    @PostMapping(value = "/import-excel-parallel-fire-and-forget", consumes = "multipart/form-data")
    @Operation(summary = "Import books from Excel (Fire-and-Forget)", description = "Same as /import/excel?strategy=virtual-threads&async=true. Returns immediately; the outcome is only logged.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Excel import accepted and processing in background"),
            @ApiResponse(responseCode = "400", description = "Not a .xlsx file"),
            @ApiResponse(responseCode = "429", description = "Another bulk operation is running")
    })
    public ResponseEntity<String> importExcelParallelFireAndForget(
            @RequestParam("file") @Parameter(description = ".xlsx file to upload", required = true) MultipartFile file) throws IOException {
        return runImport(file, VirtualThreadsImportStrategy.STRATEGY_NAME, true);
    }

    @PostMapping(value = "/import-excel-parallel-stream", consumes = "multipart/form-data")
    @Operation(summary = "Import books from Excel (Parallel Streams)", description = "Same as /import/excel?strategy=parallel-stream")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Excel file imported successfully"),
            @ApiResponse(responseCode = "400", description = "Not a valid .xlsx file"),
            @ApiResponse(responseCode = "429", description = "Another bulk operation is running")
    })
    public ResponseEntity<String> importExcelParallelStream(
            @RequestParam("file") @Parameter(description = ".xlsx file to upload", required = true) MultipartFile file) throws IOException {
        return runImport(file, ParallelStreamImportStrategy.STRATEGY_NAME, false);
    }
}
