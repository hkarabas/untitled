# CSV Import Methods Comparison Guide

## Overview
This application provides **4 different methods** for importing CSV files, each with different threading models and performance characteristics.

## Available Methods

### 1. Traditional (Single-threaded)
**Endpoint:** `POST /api/books/import-csv-traditional`

**Characteristics:**
- Single-threaded, sequential processing
- Low memory usage
- Baseline performance
- Good for small files (<10K records)

**Threading Model:** Single thread
**Expected Throughput:** ~20,000 books/sec
**Memory Usage:** Low
**Response Type:** Synchronous (waits for completion)

**Example:**
```bash
curl -X POST http://localhost:8080/api/books/import-csv-traditional \
  -F "file=@books_100000.csv"
```

---

### 2. Virtual Threads
**Endpoint:** `POST /api/books/import-csv-virtual-threads`

**Characteristics:**
- Uses Java 21 Virtual Threads
- Concurrent batch processing (20 batches concurrently)
- Medium memory usage
- Excellent for large files (100K-1M+ records)

**Threading Model:** Virtual threads (lightweight)
**Expected Throughput:** ~80,000 books/sec
**Memory Usage:** Medium
**Response Type:** Synchronous (waits for completion)

**Example:**
```bash
curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads \
  -F "file=@books_1000000.csv"
```

---

### 3. Parallel Streams (NEW!)
**Endpoint:** `POST /api/books/import-csv-parallel-stream`

**Characteristics:**
- Uses Java Streams API with `parallelStream()`
- Automatic work distribution via ForkJoinPool
- Leverages all available CPU cores
- High memory usage (loads entire file)
- Functional programming style
- Good for moderate to large files

**Threading Model:** ForkJoinPool (platform threads)
**Expected Throughput:** ~100,000 books/sec
**Memory Usage:** High (loads entire file into memory)
**Response Type:** Synchronous (waits for completion)

**Example:**
```bash
curl -X POST http://localhost:8080/api/books/import-csv-parallel-stream \
  -F "file=@books_1000000.csv"
```

**Key Features:**
- Automatic parallelization across CPU cores
- Simple, declarative API
- Filter/map operations for data processing
- Best for files that fit in memory

---

### 4. Fire-and-Forget Parallel
**Endpoint:** `POST /api/books/import-csv-parallel-fire-and-forget`

**Characteristics:**
- Asynchronous processing (returns immediately)
- Divide-and-conquer strategy
- Uses virtual threads with high concurrency (15 concurrent chunks)
- Returns 202 Accepted immediately
- Processing continues in background

**Threading Model:** Virtual threads (daemon)
**Expected Throughput:** ~100,000 books/sec
**Memory Usage:** Medium
**Response Type:** Asynchronous (202 response, processing in background)

**Example:**
```bash
curl -X POST http://localhost:8080/api/books/import-csv-parallel-fire-and-forget \
  -F "file=@books_1000000.csv"
```

**Key Features:**
- Non-blocking (immediate response)
- Perfect for web APIs (no client timeout)
- Background processing with cleanup
- Monitor progress via logs

---

## Performance Comparison Table

| Method | Threading | Concurrency | Memory | Throughput | Wait Time | Best For |
|--------|-----------|-------------|--------|-----------|-----------|----------|
| **Traditional** | Single | 1 | Low | 20K/sec | Sync | Small files |
| **Virtual Threads** | Virtual | 20 batches | Medium | 80K/sec | Sync | Large files |
| **Parallel Streams** | ForkJoinPool | CPU cores | High | 100K/sec | Sync | Medium files |
| **Fire-and-Forget** | Virtual (daemon) | 15 chunks | Medium | 100K/sec | Async | Any size |

## When to Use Each Method

### Use Traditional When:
- File size < 10,000 records
- Memory is constrained
- Simple, predictable behavior needed
- Learning/debugging

### Use Virtual Threads When:
- File size: 100K - 1M+ records
- Need synchronous response with timing
- Want excellent performance without complexity
- Have database connection pool limits

### Use Parallel Streams When:
- File fits comfortably in memory
- Want automatic CPU utilization
- Prefer functional programming style
- Need simple, clean code
- File size: 50K - 500K records

### Use Fire-and-Forget When:
- Any file size (especially very large)
- Client can't wait for response
- Avoid HTTP timeouts
- Background processing acceptable
- Web API with multiple concurrent uploads

## Code Examples

### Traditional
```java
// Sequential processing
for (String line : lines) {
    Book book = parseLine(line);
    batch.add(book);
    if (batch.size() >= 5000) {
        saveBooks(batch);
    }
}
```

### Virtual Threads
```java
// Concurrent batch processing
try (ExecutorService executor = 
    Executors.newFixedThreadPool(20, Thread.ofVirtual().factory())) {
    for (int i = 0; i < totalBatches; i++) {
        executor.submit(() -> processBatch(batch));
    }
}
```

### Parallel Streams
```java
// Functional parallel processing
List<Book> books = lines.parallelStream()
    .filter(line -> !line.isEmpty())
    .map(this::parseLine)
    .filter(Objects::nonNull)
    .toList();
```

### Fire-and-Forget
```java
// Async background processing
Thread.ofVirtual().start(() -> {
    processFile(filePath);
    cleanup();
});
// Returns immediately
```

## Testing All Methods

Run the comprehensive test script:
```bash
./test_parallel_stream.sh
```

This will:
1. Test all 4 methods with the same file
2. Compare performance metrics
3. Display response times
4. Verify data integrity

## Monitoring Performance

### View detailed logs:
```bash
# All import operations
docker-compose logs app | grep "CSV IMPORT"

# Specific method
docker-compose logs app | grep "Parallel Streams"

# Performance metrics
docker-compose logs app | grep "Throughput"
```

### Check database:
```bash
# Count books
curl -s http://localhost:8080/api/books | jq 'length'

# View sample
curl -s http://localhost:8080/api/books | jq '.[0:5]'
```

## CSV File Format

All methods expect the same CSV format:
```csv
title,author,price
The Great Adventure,John Smith,29.99
Amazing Journey,Jane Doe,19.99
"Book With, Comma",Bob Johnson,24.99
```

**Features:**
- Supports quoted fields (for titles/authors with commas)
- Validates price format (BigDecimal)
- Skips invalid records
- Removes quotes from values

## Performance Tips

### For Traditional:
- Use smaller batch sizes (1000-2000)
- Good for debugging

### For Virtual Threads:
- Adjust `maxConcurrentBatches` based on DB connection pool
- Default: 20 concurrent batches
- Increase for more concurrency

### For Parallel Streams:
- Ensure sufficient heap memory
- `-Xmx4g` recommended for large files
- Adjust batch size based on memory

### For Fire-and-Forget:
- Monitor background processing via logs
- Check disk space for temp files
- Verify cleanup happens

## Error Handling

All methods handle:
- Empty files
- Invalid CSV format
- Invalid price values
- Missing fields
- File upload errors

**Traditional/Virtual/Parallel Stream:**
- Returns 400 Bad Request with error message
- No partial imports (transaction rollback)

**Fire-and-Forget:**
- Returns 202 Accepted (file accepted)
- Errors logged to console
- Partial imports possible (per-chunk transactions)

## Memory Considerations

### File Loading Strategy:

**Traditional:** Stream-based (BufferedReader)
- Memory: ~constant (buffer size only)
- Best for: Any file size

**Virtual Threads:** Full load (Files.readAllLines)
- Memory: ~file size in RAM
- Best for: Files < 500MB

**Parallel Streams:** Full load + processing
- Memory: ~2x file size (file + book objects)
- Best for: Files < 250MB

**Fire-and-Forget:** Full load (Files.readAllLines)
- Memory: ~file size in RAM
- Best for: Files < 500MB

## API Documentation

All endpoints are documented in OpenAPI/Swagger:
```
http://localhost:8080/swagger-ui/index.html
```

## Example Workflow

```bash
# 1. Generate test data
./generate_sample_csv.sh 100000

# 2. Start application
docker-compose up -d

# 3. Wait for startup
sleep 10

# 4. Run comprehensive test
./test_parallel_stream.sh

# 5. View detailed logs
docker-compose logs app | grep "CSV IMPORT" | tail -100

# 6. Verify results
curl -s http://localhost:8080/api/books | jq 'length'
```

## Troubleshooting

### Import too slow?
- Use Virtual Threads or Parallel Streams
- Increase batch size
- Check database connection pool settings

### Out of memory?
- Use Traditional method (stream-based)
- Reduce batch size
- Increase JVM heap: `-Xmx4g`

### Client timeout?
- Use Fire-and-Forget method (async)
- Increase client timeout
- Monitor via logs instead

### Database connection errors?
- Reduce concurrent batches
- Increase connection pool size
- Use Traditional method

## Next Steps

1. **Benchmark with your data:** Test all methods with your actual file sizes
2. **Tune parameters:** Adjust batch sizes and concurrency levels
3. **Monitor production:** Track throughput and errors
4. **Scale horizontally:** Multiple app instances for even higher throughput

## Conclusion

The new **Parallel Streams** method provides:
- ✅ Simple, declarative API
- ✅ Automatic CPU utilization
- ✅ Excellent performance (~100K books/sec)
- ✅ Functional programming style
- ✅ Good for moderate-sized files

Choose the method that best fits your use case!

