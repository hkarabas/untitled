# Books API - Quick Reference Card

## Base URL
```
Development:  http://localhost:8080
Production:   https://api.example.com
```

## API Endpoints Quick Reference

### 📚 CRUD Operations

**Get All Books**
```bash
GET /api/books
```

**Get Book by ID**
```bash
GET /api/books/{id}
```

**Create Book**
```bash
POST /api/books
Content-Type: application/json

{
  "title": "Book Title",
  "author": "Author Name",
  "price": 29.99
}
```

**Delete Book**
```bash
DELETE /api/books/{id}
```

---

### 🔧 Data Operations

**Generate Random Books**
```bash
POST /api/books/generate-random?count=100000
```

**Import CSV (Traditional - Single-threaded)**
```bash
POST /api/books/import-csv-traditional
Content-Type: multipart/form-data

file: [CSV file]
```

**Import CSV (Virtual Threads - Concurrent)**
```bash
POST /api/books/import-csv-virtual-threads
Content-Type: multipart/form-data

file: [CSV file]
```

---

## CSV Format

```csv
title,author,price
"The Great Adventure","John Smith",29.99
"Silent Journey","Sarah Johnson",34.99
```

**Requirements:**
- Header: `title,author,price`
- Quotes optional (required if field contains comma)
- Price: decimal format (e.g., 29.99)

---

## HTTP Status Codes

| Code | Meaning | Common Causes |
|------|---------|---------------|
| 200 | OK | Success |
| 201 | Created | Resource created |
| 204 | No Content | Delete success |
| 400 | Bad Request | Invalid input, file error |
| 404 | Not Found | Resource not found |
| 500 | Server Error | Server problem |

---

## Common Errors

| Error Message | Solution |
|---------------|----------|
| "Only CSV files are allowed" | Rename file to `.csv` extension |
| "Please select a CSV file to upload" | Check file is not empty |
| "CSV import failed: Invalid price..." | Fix CSV format, ensure valid prices |
| 404 on GET | Check book ID exists |

---

## Performance Comparison

| Operation | Time | Records | Method |
|-----------|------|---------|--------|
| Import CSV | ~15s | 100K | Traditional |
| Import CSV | ~8s | 100K | Virtual Threads |
| Generate | ~45s | 100K | Virtual Threads |
| Generate | ~3-5min | 1M | Virtual Threads |

**Recommendation:**
- Use **Virtual Threads** for files > 100K records
- Use **Traditional** for smaller files or debugging

---

## Curl Examples

### Get All Books
```bash
curl http://localhost:8080/api/books
```

### Create Book
```bash
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"My Book","author":"Me","price":19.99}'
```

### Import CSV (Virtual Threads)
```bash
curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads \
  -F "file=@books.csv"
```

### Generate 1M Books
```bash
curl -X POST "http://localhost:8080/api/books/generate-random?count=1000000"
```

---

## JavaScript Fetch Examples

### Get All Books
```javascript
fetch('http://localhost:8080/api/books')
  .then(r => r.json())
  .then(books => console.log(books))
```

### Create Book
```javascript
fetch('http://localhost:8080/api/books', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({
    title: 'My Book',
    author: 'Me',
    price: 19.99
  })
})
.then(r => r.json())
.then(book => console.log('Created:', book))
```

### Import CSV
```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch('http://localhost:8080/api/books/import-csv-virtual-threads', {
  method: 'POST',
  body: formData
})
.then(r => r.text())
.then(msg => console.log(msg))
```

---

## Documentation Links

| Resource | URL |
|----------|-----|
| Swagger UI | `/swagger-ui.html` |
| OpenAPI JSON | `/v3/api-docs` |
| OpenAPI YAML | `/v3/api-docs.yaml` |
| Full Docs | See `OPENAPI_DOCUMENTATION.md` |
| Usage Guide | See `API_USAGE_GUIDE.md` |

---

## Data Model

```json
Book {
  id: Long (auto-generated),
  title: String (required),
  author: String (required),
  price: BigDecimal (required, 2 decimals)
}
```

**Example:**
```json
{
  "id": 1,
  "title": "The Great Adventure",
  "author": "John Smith",
  "price": 29.99
}
```

---

## Request/Response Headers

### Request Headers
- **Content-Type:** `application/json` (for POST/PUT with body)
- **Content-Type:** `multipart/form-data` (for CSV uploads)

### Response Headers
- **Content-Type:** `application/json` (default)
- **Content-Type:** `text/plain` (for messages)

---

## Troubleshooting Checklist

- [ ] API running on correct port (8080)?
- [ ] CSV file has `.csv` extension?
- [ ] CSV not empty?
- [ ] CSV has header row: `title,author,price`?
- [ ] Prices are valid decimal format (e.g., 29.99)?
- [ ] JSON Content-Type header set for POST body?
- [ ] Book ID exists before GET/DELETE?

---

## Performance Tips

1. ✅ Use **Virtual Threads** endpoint for large files
2. ✅ Monitor logs for import progress
3. ✅ Batch CSV operations if possible
4. ✅ Check database connection pool
5. ✅ Monitor disk space before large imports
6. ✅ Use reasonable batch sizes (5K-10K records)

---

## Environment Variables

### Optional Configuration
```bash
# Increase JVM heap (for large imports)
export JAVA_OPTS="-Xmx2g"

# Database connection pool (if using remote DB)
export DB_URL="jdbc:mysql://localhost:3306/books"
export DB_USER="user"
export DB_PASSWORD="password"
```

---

## Quick Start Commands

```bash
# Start the application
mvn spring-boot:run

# Access Swagger UI
open http://localhost:8080/swagger-ui.html

# Test with curl
curl http://localhost:8080/api/books

# Create a test CSV and import
echo "title,author,price" > test.csv
echo "Test Book,Test Author,19.99" >> test.csv
curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads \
  -F "file=@test.csv"
```

---

## Version
- **API Version:** 1.0.0
- **Last Updated:** January 15, 2026
- **License:** Apache 2.0

---

## Support
- **Email:** support@example.com
- **Website:** https://example.com

For detailed documentation, see `OPENAPI_DOCUMENTATION.md`
For usage examples, see `API_USAGE_GUIDE.md`

