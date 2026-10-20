# CSV Import Methods Updated - File Upload Support

## Changes Made

Both CSV import endpoints have been updated to accept **file uploads** instead of file paths.

---

## What Changed

### Before (File Path Parameter)
```java
@PostMapping("/import-csv-traditional")
public ResponseEntity<String> importCsvTraditional(
    @RequestParam String filePath) {
    // Import from file path
}

@PostMapping("/import-csv-virtual-threads")
public ResponseEntity<String> importCsvVirtualThreads(
    @RequestParam String filePath) {
    // Import from file path
}
```

### After (File Upload via MultipartFile)
```java
@PostMapping(value = "/import-csv-traditional", consumes = "multipart/form-data")
public ResponseEntity<String> importCsvTraditional(
    @RequestParam("file") MultipartFile file) {
    // 1. Validate file (not empty, must be .csv)
    // 2. Save to temporary location
    // 3. Import from temp file
    // 4. Clean up temp file
    // 5. Return timing info
}

@PostMapping(value = "/import-csv-virtual-threads", consumes = "multipart/form-data")
public ResponseEntity<String> importCsvVirtualThreads(
    @RequestParam("file") MultipartFile file) {
    // Same workflow as traditional
}
```

---

## New Features

### ✅ File Upload Support
- Users can now upload CSV files directly through the API
- Works with Swagger UI file upload widget
- Works with Postman, cURL, and other HTTP clients

### ✅ File Validation
- Checks if file is empty
- Validates file extension (.csv only)
- Returns clear error messages

### ✅ Temporary File Handling
- Uploaded files are saved to temporary location
- Processed from temp location
- Automatically cleaned up after processing
- Error handling for cleanup failures

### ✅ Performance Timing
- Shows import duration in milliseconds
- Shows duration in seconds
- Includes timing in response message

---

## How to Use

### Via Swagger UI

1. **Open Swagger UI**
   ```
   http://localhost:8080/swagger-ui/index.html
   ```

2. **Navigate to CSV Import Endpoint**
   - Expand `/api/books/import-csv-traditional` or
   - Expand `/api/books/import-csv-virtual-threads`

3. **Click "Try it out"**

4. **Upload File**
   - Click "Choose File" button
   - Select your CSV file
   - Click "Execute"

5. **View Response**
   - See success message with timing
   - Or error message if validation fails

### Via cURL

```bash
# Traditional method
curl -X POST http://localhost:8080/api/books/import-csv-traditional \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/your/books.csv"

# Virtual threads method
curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/your/books.csv"
```

### Via Postman

1. Create new POST request
2. URL: `http://localhost:8080/api/books/import-csv-virtual-threads`
3. Go to "Body" tab
4. Select "form-data"
5. Add key: `file` (change type to "File")
6. Choose your CSV file
7. Click "Send"

---

## Response Examples

### Success Response
```json
HTTP/1.1 200 OK
Content-Type: text/plain

CSV import completed successfully (Virtual Threads method) in 2543 ms (2.54 seconds)
```

### Empty File Error
```json
HTTP/1.1 400 Bad Request
Content-Type: text/plain

Please select a CSV file to upload
```

### Invalid File Type Error
```json
HTTP/1.1 400 Bad Request
Content-Type: text/plain

Only CSV files are allowed
```

### Import Error
```json
HTTP/1.1 400 Bad Request
Content-Type: text/plain

CSV import failed: Invalid CSV format at line 42
```

---

## Technical Details

### New Imports Added
```java
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
```

### Workflow
1. **Receive Upload**: Accept MultipartFile parameter
2. **Validate**: Check empty and file extension
3. **Create Temp File**: `Files.createTempFile("books_upload_", ".csv")`
4. **Copy Upload**: `Files.copy(file.getInputStream(), tempFile, ...)`
5. **Process**: Call existing service method with temp file path
6. **Measure Time**: Track duration of import
7. **Clean Up**: Delete temp file in finally block
8. **Return Result**: Success message with timing or error

### Error Handling
- **IOException**: File read/write errors
- **Exception**: General import errors (CSV parsing, database issues)
- **Finally Block**: Ensures temp file cleanup even on error

### File Validation
- Checks `file.isEmpty()` - Rejects empty uploads
- Checks file extension - Only accepts `.csv` files (case-insensitive)
- Provides clear error messages for validation failures

---

## Endpoints Updated

| Endpoint | Method | Accepts | Returns |
|----------|--------|---------|---------|
| `/api/books/import-csv-traditional` | POST | multipart/form-data | Timing info or error |
| `/api/books/import-csv-virtual-threads` | POST | multipart/form-data | Timing info or error |

---

## Benefits

### ✅ User-Friendly
- No need to specify file paths
- Upload directly from browser or API client
- Works seamlessly with Swagger UI

### ✅ Secure
- Files are validated before processing
- Temporary files are cleaned up
- No file system path exposure

### ✅ Flexible
- Works with any HTTP client
- Supports automation scripts
- Can be called from frontend applications

### ✅ Professional
- Proper error messages
- Performance timing included
- OpenAPI documented in Swagger UI

---

## Testing

### Test with Sample CSV
Create a file `books.csv`:
```csv
title,author,price
Clean Code,Robert C. Martin,45.99
Effective Java,Joshua Bloch,49.99
Design Patterns,Gang of Four,54.99
```

### Test via cURL
```bash
curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads \
  -H "Content-Type: multipart/form-data" \
  -F "file=@books.csv"
```

### Expected Response
```
CSV import completed successfully (Virtual Threads method) in 125 ms (0.13 seconds)
```

---

## Next Steps

1. **Rebuild Docker** (if needed)
   ```bash
   mvn clean package -DskipTests
   docker-compose down
   docker-compose build --no-cache
   docker-compose up -d
   ```

2. **Test in Swagger UI**
   - Open http://localhost:8080/swagger-ui/index.html
   - Try the file upload feature

3. **Test with Real Data**
   - Upload your actual CSV files
   - Compare traditional vs virtual threads performance

---

## Comparison: Traditional vs Virtual Threads

Both methods now accept file uploads the same way. The difference is in processing:

| Feature | Traditional | Virtual Threads |
|---------|-------------|-----------------|
| **Upload** | ✅ File upload | ✅ File upload |
| **Validation** | ✅ Same | ✅ Same |
| **Temp File** | ✅ Same | ✅ Same |
| **Processing** | Platform threads | Virtual threads |
| **Performance** | Good for small files | Better for large files |
| **Concurrency** | Limited by thread pool | Scales better |

---

## Files Modified

- `src/main/java/com/example/untitled/controller/BookController.java`
  - Updated imports
  - Changed `importCsvTraditional()` method
  - Changed `importCsvVirtualThreads()` method

---

## Status

✅ **Changes Complete**
- Both methods updated to accept file uploads
- File validation added
- Temporary file handling implemented
- Error handling improved
- Performance timing included
- OpenAPI annotations updated

✅ **Ready to Use**
- Build and deploy
- Test with Swagger UI
- Use with any HTTP client

---

**Enjoy the improved CSV import with file upload support!** 🚀

