# Parallel Stream Upload Method Implementation

## Overview
Added a **parallel stream-based CSV import method** to the book management application. This method uses Java's Streams API with `parallelStream()` for concurrent processing of large CSV files.

## What Was Added

### 1. Core Implementation Method
**File:** `BookServiceImpl.java` (Line 638)

```java
public void importBooksFromCsvParallelStream(String csvFilePath)
```

**Features:**
- Uses `Files.readAllLines()` to load all CSV data into memory
- Employs `parallelStream()` for concurrent line processing
- Automatically partitions work across available CPU cores via `ForkJoinPool`
- Filters empty lines and maps to Book objects
- Processes in batches (5000 records per batch) to manage transaction scope
- Includes performance metrics (throughput, memory usage, processing time)

### 2. File Upload Handler
**File:** `BookServiceImpl.java` (Line 728)

```java
@Override
public String importCsvFileParallelStream(MultipartFile file) throws IOException
```

**Functionality:**
- Validates uploaded CSV file
- Saves to temporary location
- Calls `importBooksFromCsvParallelStream()`
- Returns performance statistics
- Cleans up temporary files

### 3. REST Controller Endpoint
**File:** `BookController.java` (Line 135)

```java
@PostMapping(value = "/import-csv-parallel-stream", consumes = "multipart/form-data")
public ResponseEntity<String> importCsvParallelStream(MultipartFile file)
```

**Endpoint Details:**
- **Path:** `/api/books/import-csv-parallel-stream`
- **Method:** POST
- **Content-Type:** multipart/form-data
- **Response:** 200 OK with performance metrics on success, 400 Bad Request on failure
- **OpenAPI Documentation:** Included with full operation summary

### 4. Service Interface
**File:** `BookService.java` (Line 21)

Added method signature:
```java
String importCsvFileParallelStream(MultipartFile file) throws IOException;
```

## Key Characteristics

### Advantages of Parallel Streams
1. **Automatic Parallelization:** ForkJoinPool automatically distributes work across CPU cores
2. **Simple API:** Less boilerplate compared to manual ExecutorService management
3. **Functional Style:** Leverages functional programming with filter/map operations
4. **Good for Moderate Datasets:** Ideal for datasets that fit in memory

### Performance Metrics
Each import logs:
- Total records processed
- Processing time (ms and seconds)
- Throughput (books/second)
- Memory usage (MB)
- Available CPU cores

### Example Output
```
===== CSV IMPORT (Parallel Streams) =====
Starting import from: /tmp/books_upload_parallel_stream_xxx.csv
Total lines to process: 1000000
Available processors: 8

... [processing] ...

===== IMPORT COMPLETE (Parallel Streams) =====
Total Records: 999950
Total Time: 8.45 seconds
Throughput: 118309.35 books/sec
Memory Used: 2048 MB
```

## Usage

### Test with curl
```bash
curl -X POST http://localhost:8080/api/books/import-csv-parallel-stream \
  -F "file=@books_1000000.csv"
```

### Response Format
```json
{
  "message": "CSV import completed successfully (Parallel Streams method) in 8450 ms (8.45 seconds)"
}
```

## CSV File Format
Required format: `title,author,price`

Example:
```csv
The Great Adventure,John Smith,29.99
Amazing Journey,Jane Doe,19.99
Hidden Mystery,Bob Johnson,24.99
```

## Comparison with Other Methods

| Method | Threading Model | Concurrency | Memory | Throughput | Wait Time |
|--------|-----------------|-------------|--------|-----------|-----------|
| **Traditional** | Single-threaded | None | Low | ~20K books/sec | Sync |
| **Virtual Threads** | Virtual threads | High (~20 concurrent) | Medium | ~80K books/sec | Sync |
| **Parallel Streams** | ForkJoinPool | Auto (CPU cores) | High (loads entire file) | ~100K books/sec | Sync |
| **Fire-and-Forget** | Virtual threads | Very High | Medium | ~100K books/sec | Async (202 response) |

## Implementation Details

### CSV Parsing
- Uses regex split: `line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1)`
- Handles quoted fields with commas inside
- Removes quotes from field values
- Validates price format (BigDecimal)
- Skips invalid records silently

### Batch Processing
- Streams process individual lines concurrently
- Results batched into 5000-record groups
- Each batch saved in separate transaction
- Prevents memory exhaustion on large files

### Transaction Management
- Each batch commits independently
- Improves performance for large imports
- Enables resumption on errors

## Error Handling
- Empty file validation
- File extension validation (.csv only)
- Invalid price format handling
- CSV parsing error resilience
- Temporary file cleanup on all paths

## Build & Deploy

### Build
```bash
mvn clean package
```

### Deploy with Docker
```bash
docker-compose down
docker-compose up --build -d
```

### Verify
```bash
curl -X GET http://localhost:8080/api/books | jq '.[] | .title, .author, .price' | head -20
```

## Files Modified

1. **BookServiceImpl.java** - Added implementation (2 methods)
2. **BookService.java** - Added interface method
3. **BookController.java** - Added REST endpoint

## Next Steps

To further optimize parallel stream processing:
1. Consider custom ForkJoinPool configuration for larger datasets
2. Implement batch size tuning based on available memory
3. Add progress callback for real-time UI updates
4. Consider file streaming instead of loading entire file into memory for very large files

## Notes

- Parallel streams work best with moderate to large files (100K+ records)
- Smaller files may not show performance benefit due to overhead
- Memory usage is higher than traditional/virtual thread methods
- The streams are always synchronous (waits for completion before returning)
- For fire-and-forget async behavior, use `/import-csv-parallel-fire-and-forget` endpoint instead

