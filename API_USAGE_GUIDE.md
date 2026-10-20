# Books API - Usage Guide & Examples

## Quick Start

### 1. Start the Application
```bash
mvn spring-boot:run
```

The API will be available at: `http://localhost:8080`

### 2. Access Swagger UI
```
http://localhost:8080/swagger-ui.html
```

---

## API Usage Examples

### Using curl

#### Get All Books
```bash
curl http://localhost:8080/api/books
```

#### Get a Specific Book
```bash
curl http://localhost:8080/api/books/1
```

#### Create a New Book
```bash
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "The Art of Programming",
    "author": "Jane Doe",
    "price": 39.99
  }'
```

#### Delete a Book
```bash
curl -X DELETE http://localhost:8080/api/books/1
```

#### Generate Random Books
```bash
# Generate 100,000 books
curl -X POST "http://localhost:8080/api/books/generate-random?count=100000"

# Generate 1,000,000 books
curl -X POST "http://localhost:8080/api/books/generate-random?count=1000000"
```

#### Import CSV (Traditional Method)
```bash
curl -X POST http://localhost:8080/api/books/import-csv-traditional \
  -F "file=@books.csv"
```

#### Import CSV (Virtual Threads Method)
```bash
curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads \
  -F "file=@books.csv"
```

---

### Using JavaScript/Fetch

#### Get All Books
```javascript
fetch('http://localhost:8080/api/books')
  .then(response => response.json())
  .then(data => console.log(data));
```

#### Create a Book
```javascript
const newBook = {
  title: "The Art of Programming",
  author: "Jane Doe",
  price: 39.99
};

fetch('http://localhost:8080/api/books', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify(newBook)
})
.then(response => response.json())
.then(data => console.log('Created:', data));
```

#### Import CSV File
```javascript
const fileInput = document.getElementById('csvFile');
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch('http://localhost:8080/api/books/import-csv-virtual-threads', {
  method: 'POST',
  body: formData
})
.then(response => response.text())
.then(data => console.log('Import result:', data));
```

#### Generate Random Books
```javascript
fetch('http://localhost:8080/api/books/generate-random?count=100000', {
  method: 'POST'
})
.then(response => response.text())
.then(data => console.log('Generation result:', data));
```

---

### Using Python

#### Get All Books
```python
import requests

response = requests.get('http://localhost:8080/api/books')
books = response.json()
print(books)
```

#### Create a Book
```python
import requests

new_book = {
    "title": "The Art of Programming",
    "author": "Jane Doe",
    "price": 39.99
}

response = requests.post(
    'http://localhost:8080/api/books',
    json=new_book
)
created_book = response.json()
print(f"Created: {created_book}")
```

#### Import CSV File
```python
import requests

files = {'file': open('books.csv', 'rb')}
response = requests.post(
    'http://localhost:8080/api/books/import-csv-virtual-threads',
    files=files
)
print(f"Import result: {response.text}")
```

#### Generate Random Books
```python
import requests

response = requests.post(
    'http://localhost:8080/api/books/generate-random?count=100000'
)
print(f"Generation result: {response.text}")
```

---

### Using Postman

#### Setup Collection

1. **Create a new collection** - "Books API"

2. **Create requests:**

**GET - Get All Books**
- Method: GET
- URL: `http://localhost:8080/api/books`

**POST - Create Book**
- Method: POST
- URL: `http://localhost:8080/api/books`
- Headers: `Content-Type: application/json`
- Body:
```json
{
  "title": "The Art of Programming",
  "author": "Jane Doe",
  "price": 39.99
}
```

**GET - Get Book by ID**
- Method: GET
- URL: `http://localhost:8080/api/books/{{bookId}}`

**DELETE - Delete Book**
- Method: DELETE
- URL: `http://localhost:8080/api/books/{{bookId}}`

**POST - Generate Random Books**
- Method: POST
- URL: `http://localhost:8080/api/books/generate-random?count=100000`

**POST - Import CSV (Virtual Threads)**
- Method: POST
- URL: `http://localhost:8080/api/books/import-csv-virtual-threads`
- Body: form-data
  - Key: `file`
  - Value: Select your CSV file

---

## CSV File Preparation

### Sample CSV File (books.csv)

```csv
title,author,price
"The Great Adventure","John Smith",29.99
"Silent Journey","Sarah Johnson",34.99
"Ancient Mysteries","Robert Davis",45.99
"Modern Science","Emily Wilson",55.99
"The Lost World","Michael Brown",39.99
```

### Generate CSV File (Linux/Mac)

```bash
# Create a simple CSV with 1000 books
cat > books.csv << 'EOF'
title,author,price
EOF

# Add 1000 sample books
for i in {1..1000}; do
  echo "\"Book Title $i\",\"Author $i\",$(printf "%.2f" $(echo "5.99 + $RANDOM/656.36" | bc))" >> books.csv
done
```

### Generate CSV File (PowerShell)

```powershell
# Create CSV header
"title,author,price" | Out-File books.csv

# Add 1000 sample books
1..1000 | ForEach-Object {
    $price = Get-Random -Minimum 599 -Maximum 9999
    $price = [math]::Round($price / 100, 2)
    "Book Title $_,Author $_,$price" | Add-Content books.csv
}
```

---

## Performance Testing

### Test Traditional vs Virtual Threads

```bash
# Create a test CSV with 100K records
echo "title,author,price" > large_books.csv
for i in {1..100000}; do
  echo "Book Title $i,Author Name $i,29.99" >> large_books.csv
done

# Time the traditional import
time curl -X POST http://localhost:8080/api/books/import-csv-traditional \
  -F "file=@large_books.csv"

# Time the virtual threads import
time curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads \
  -F "file=@large_books.csv"
```

**Expected Results:**
- Traditional: ~45 seconds for 100K records
- Virtual Threads: ~8-12 seconds for 100K records
- Speedup: 4-5x faster

---

## Common Issues & Troubleshooting

### Issue: "Only CSV files are allowed"
**Solution:** Ensure your file has a `.csv` extension
```bash
# Rename if needed
mv myfile.txt myfile.csv
```

### Issue: "Please select a CSV file to upload"
**Solution:** Ensure you're selecting a non-empty file
```bash
# Check file size
ls -lh books.csv
```

### Issue: Invalid price errors
**Solution:** Ensure CSV has proper format:
```csv
# ✅ Correct format
title,author,price
"My Book","John Doe",29.99

# ❌ Wrong format (no quotes, price issues)
My Book,John Doe,29.99a
```

### Issue: Timeout on large imports
**Solution:** Increase Spring Boot timeout:
```properties
# application.properties
server.tomcat.connection-timeout=600000
```

### Issue: Out of memory errors
**Solution:** Increase JVM heap size:
```bash
export JAVA_OPTS="-Xmx2g"
mvn spring-boot:run
```

---

## Performance Tips

1. **Use Virtual Threads** for files larger than 100K records
2. **Monitor logs** for import progress
3. **Batch your requests** when importing multiple files
4. **Check database** connection pool configuration
5. **Monitor disk space** before importing large files

---

## API Response Examples

### Success Response (200 OK)
```bash
curl http://localhost:8080/api/books/1
```
```json
{
  "id": 1,
  "title": "The Great Adventure",
  "author": "John Smith",
  "price": 29.99
}
```

### Error Response (400 Bad Request)
```bash
curl -X POST http://localhost:8080/api/books/import-csv-traditional \
  -F "file=@notacsv.txt"
```
```json
"Only CSV files are allowed"
```

### Error Response (404 Not Found)
```bash
curl http://localhost:8080/api/books/999
```
```json
```
(Empty response with 404 status)

---

## Next Steps

1. ✅ Start the application
2. ✅ Access Swagger UI at `/swagger-ui.html`
3. ✅ Test each endpoint
4. ✅ Upload your CSV files
5. ✅ Monitor performance logs

For more details, see `OPENAPI_DOCUMENTATION.md`

