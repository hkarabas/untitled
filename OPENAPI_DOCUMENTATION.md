# OpenAPI Documentation - Books API v1.0.0

## Overview
The Books API is a high-performance REST API for managing a library of books with comprehensive CRUD operations, bulk CSV imports (traditional and virtual threads), and optimized data generation.

**Base URL:** `http://localhost:8080/api/books`

**Production URL:** `https://api.example.com/api/books`

## API Endpoints

### 1. Get All Books
**Retrieve a list of all books from the database**

```
GET /api/books
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "title": "The Great Adventure",
    "author": "John Smith",
    "price": 29.99
  },
  {
    "id": 2,
    "title": "Silent Journey",
    "author": "Sarah Johnson",
    "price": 34.99
  }
]
```

**Status Codes:**
- `200 OK` - Successfully retrieved list of books

---

### 2. Get Book by ID
**Retrieve a specific book by its ID**

```
GET /api/books/{id}
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | The unique identifier of the book |

**Response:** `200 OK`
```json
{
  "id": 1,
  "title": "The Great Adventure",
  "author": "John Smith",
  "price": 29.99
}
```

**Status Codes:**
- `200 OK` - Book found
- `404 Not Found` - Book not found

**Example:**
```bash
curl http://localhost:8080/api/books/1
```

---

### 3. Create a New Book
**Add a new book to the database**

```
POST /api/books
Content-Type: application/json
```

**Request Body:**
```json
{
  "title": "New Book Title",
  "author": "Author Name",
  "price": 19.99
}
```

**Response:** `201 Created`
```json
{
  "id": 3,
  "title": "New Book Title",
  "author": "Author Name",
  "price": 19.99
}
```

**Status Codes:**
- `201 Created` - Book created successfully

**Example:**
```bash
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "New Book Title",
    "author": "Author Name",
    "price": 19.99
  }'
```

---

### 4. Delete a Book
**Remove a book from the database by its ID**

```
DELETE /api/books/{id}
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | The unique identifier of the book to delete |

**Response:** `204 No Content`

**Status Codes:**
- `204 No Content` - Book deleted successfully
- `404 Not Found` - Book not found

**Example:**
```bash
curl -X DELETE http://localhost:8080/api/books/1
```

---

### 5. Generate Random Books
**Generate a specified number of random books in the database**

```
POST /api/books/generate-random?count=100000
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `count` | Integer | 100000 | Number of books to generate |

**Response:** `200 OK`
```json
"Successfully generated 100000 random books in 45000 ms (45.00 seconds)"
```

**Status Codes:**
- `200 OK` - Books generated successfully

**Features:**
- Uses virtual threads for optimal performance
- Generates realistic book titles and authors
- Random prices between $5.99 and $99.99
- Batch processing to optimize database writes
- Progress tracking in logs

**Example:**
```bash
# Generate 100,000 books (default)
curl -X POST http://localhost:8080/api/books/generate-random

# Generate 1,000,000 books
curl -X POST "http://localhost:8080/api/books/generate-random?count=1000000"

# Generate 10,000 books
curl -X POST "http://localhost:8080/api/books/generate-random?count=10000"
```

**Performance Notes:**
- Small datasets (< 10K): < 1 second
- Medium datasets (100K): 30-60 seconds
- Large datasets (1M): 3-5 minutes
- Uses connection pooling and batch inserts

---

### 6. Import Books from CSV (Traditional)
**Upload and import books from a CSV file using traditional threading**

```
POST /api/books/import-csv-traditional
Content-Type: multipart/form-data
```

**Request Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | Yes | CSV file to upload (.csv extension required) |

**CSV Format:**
```csv
title,author,price
"The Great Adventure","John Smith",29.99
"Silent Journey","Sarah Johnson",34.99
"Ancient Mysteries","Robert Davis",45.99
```

**Response:** `200 OK`
```json
"CSV import completed successfully (Traditional method) in 15000 ms (15.00 seconds)"
```

**Status Codes:**
- `200 OK` - CSV imported successfully
- `400 Bad Request` - File validation failed or import error

**Error Responses:**
```json
"Please select a CSV file to upload"
"Only CSV files are allowed"
"CSV import failed: [error details]"
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/books/import-csv-traditional \
  -F "file=@books.csv"
```

**Features:**
- Single-threaded processing
- CSV validation (file type, emptiness)
- Batch processing (5000 records per batch)
- Automatic temporary file cleanup
- Error reporting with line numbers
- Performance metrics in response

**CSV Requirements:**
- Header row: `title,author,price`
- Title and author can be quoted for CSV parsing
- Price must be valid decimal format
- One record per line

---

### 7. Import Books from CSV (Virtual Threads)
**Upload and import books from a CSV file using virtual threads for better performance**

```
POST /api/books/import-csv-virtual-threads
Content-Type: multipart/form-data
```

**Request Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | Yes | CSV file to upload (.csv extension required) |

**CSV Format:**
Same as Traditional import (see above)

**Response:** `200 OK`
```json
"CSV import completed successfully (Virtual Threads method) in 8000 ms (8.00 seconds)"
```

**Status Codes:**
- `200 OK` - CSV imported successfully
- `400 Bad Request` - File validation failed or import error

**Error Responses:**
```json
"Please select a CSV file to upload"
"Only CSV files are allowed"
"CSV import failed: [error details]"
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads \
  -F "file=@books.csv"
```

**Features:**
- Virtual threads for concurrent processing (Java 21+)
- Up to 10 concurrent batches
- CSV validation (file type, emptiness)
- Batch processing (5000 records per batch)
- Automatic temporary file cleanup
- Real-time progress tracking
- Performance metrics in response

**Performance Comparison:**
- **Traditional Method**: Best for small files, simpler processing
- **Virtual Threads Method**: 40-80% faster for large files (100K+ records)
- **Throughput**: Up to 100K records/sec with virtual threads

---

## Error Handling

### Common Error Responses

**400 Bad Request - Empty File**
```json
"Please select a CSV file to upload"
```

**400 Bad Request - Invalid File Type**
```json
"Only CSV files are allowed"
```

**400 Bad Request - CSV Parse Error**
```json
"CSV import failed: Invalid price at line 5: The Book,Author Name,invalid_price"
```

**404 Not Found**
```json
{
  "error": "Not Found",
  "message": "Book with id 999 not found"
}
```

---

## CSV Import Formats

### Valid CSV Examples

**Basic Format:**
```csv
title,author,price
The Great Adventure,John Smith,29.99
Silent Journey,Sarah Johnson,34.99
```

**With Quoted Fields:**
```csv
title,author,price
"The Great Adventure","John Smith",29.99
"The ""Special"" Book","Sarah ""Sally"" Johnson",34.99
```

**With Commas in Fields:**
```csv
title,author,price
"The Great, Amazing Adventure","Smith, John",29.99
"A Book, Vol. 1","Johnson, Sarah",34.99
```

### Invalid CSV Examples

❌ Missing price (will be skipped):
```csv
title,author,price
The Great Adventure,John Smith,
```

❌ Invalid price format (will be skipped):
```csv
title,author,price
The Great Adventure,John Smith,ABC
```

❌ Missing header row (will cause errors):
```csv
The Great Adventure,John Smith,29.99
Silent Journey,Sarah Johnson,34.99
```

---

## Data Models

### Book Entity
```json
{
  "id": 1,
  "title": "The Great Adventure",
  "author": "John Smith",
  "price": 29.99
}
```

**Field Descriptions:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Unique identifier (auto-generated) |
| `title` | String | Book title (required) |
| `author` | String | Author name (required) |
| `price` | BigDecimal | Book price in USD (required, 2 decimal places) |

---

## Rate Limiting & Performance

### Recommended Limits
- **CSV File Size**: Up to 100MB
- **Batch Size**: 5000 records per batch
- **Concurrent Batches**: Up to 10 (configurable)
- **Timeout**: 10 minutes for large imports

### Performance Tips
1. Use **Virtual Threads** endpoint for files > 100K records
2. Use **Traditional** endpoint for small files or simple operations
3. Monitor database connection pool usage
4. Batch your requests if importing multiple files
5. Monitor logs for detailed progress information

---

## Accessing OpenAPI Documentation

### Swagger UI
Access the interactive OpenAPI documentation at:
```
http://localhost:8080/swagger-ui.html
```

### OpenAPI JSON
Download the raw OpenAPI specification:
```
http://localhost:8080/v3/api-docs
```

### OpenAPI YAML
Download the YAML format specification:
```
http://localhost:8080/v3/api-docs.yaml
```

---

## Authentication & Security

Currently, the API has **no authentication** enabled. For production, implement:
- OAuth 2.0 / OpenID Connect
- JWT Bearer tokens
- API key validation
- CORS policies
- Rate limiting

---

## Version History

### v1.0.0 (January 15, 2026)
- ✅ CRUD operations for books
- ✅ CSV import with traditional threading
- ✅ CSV import with virtual threads (Java 21+)
- ✅ Random book generation
- ✅ OpenAPI 3.0 documentation

---

## Contact & Support

- **Email**: support@example.com
- **Website**: https://example.com
- **Issues**: https://github.com/example/books-api

---

## License

This API is provided under the **Apache 2.0 License**.
See LICENSE file for details.

