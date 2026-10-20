# ✅ CSV File Upload - Build and Deploy Guide

## Status: Code Changes Complete

The BookController.java has been updated with file upload support for CSV imports.

---

## What Was Changed

### ✅ Imports Added
```java
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
```

### ✅ Methods Updated
- `importCsvTraditional()` - Now accepts MultipartFile
- `importCsvVirtualThreads()` - Now accepts MultipartFile

Both methods now:
1. Accept file uploads via `multipart/form-data`
2. Validate file (not empty, must be .csv)
3. Save to temporary location
4. Process the import
5. Clean up temporary file
6. Return timing information

---

## Build and Deploy Steps

### Step 1: Build with Maven
```bash
cd /Users/hkarabas/IdeaProjects/untitled
mvn clean package -DskipTests
```

**Expected Result:**
```
[INFO] BUILD SUCCESS
[INFO] Building jar: .../target/untitled-0.0.1-SNAPSHOT.jar
```

### Step 2: Stop Docker
```bash
docker-compose down
```

### Step 3: Rebuild Docker (No Cache)
```bash
docker-compose build --no-cache app
```

**Why no-cache?** Ensures all new code changes are included in the Docker image.

### Step 4: Start Docker
```bash
docker-compose up -d
```

### Step 5: Verify
```bash
# Wait for startup
sleep 15

# Check health
curl http://localhost:8080/health

# Check OpenAPI
curl http://localhost:8080/v3/api-docs | head -20

# Check Swagger UI
curl -I http://localhost:8080/swagger-ui/index.html
```

---

## Quick Deploy (One Command)

Use the provided script:
```bash
./build_and_deploy.sh
```

This script will:
1. ✅ Build with Maven
2. ✅ Stop Docker containers
3. ✅ Rebuild Docker images (no cache)
4. ✅ Start Docker containers
5. ✅ Wait for startup
6. ✅ Verify all endpoints
7. ✅ Show access URLs

---

## Verify File Upload Works

### Via Swagger UI
1. Open: `http://localhost:8080/swagger-ui/index.html`
2. Navigate to: `/api/books/import-csv-virtual-threads`
3. Click "Try it out"
4. You should see a **"Choose File"** button
5. Select a CSV file
6. Click "Execute"
7. See response with timing

### Via cURL
```bash
# Create test CSV
cat > test_books.csv << 'EOF'
title,author,price
Test Book 1,Author 1,29.99
Test Book 2,Author 2,39.99
EOF

# Upload file
curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads \
  -H "Content-Type: multipart/form-data" \
  -F "file=@test_books.csv"
```

**Expected Response:**
```
CSV import completed successfully (Virtual Threads method) in 156 ms (0.16 seconds)
```

### Via Postman
1. Create new POST request
2. URL: `http://localhost:8080/api/books/import-csv-virtual-threads`
3. Body tab → form-data
4. Key: `file` (select "File" type from dropdown)
5. Value: Choose your CSV file
6. Send

---

## Expected Swagger UI Changes

After deployment, in Swagger UI you'll see:

### Before (Old)
```
Parameter: filePath (string)
Type: query parameter
Example: /path/to/file.csv
```

### After (New)
```
Parameter: file (file)
Type: multipart/form-data
UI: File upload button
```

---

## Troubleshooting

### If Maven Build Fails

**Check imports:**
```bash
head -25 src/main/java/com/example/untitled/controller/BookController.java
```

Should include:
- `import org.springframework.web.multipart.MultipartFile;`
- `import java.io.IOException;`
- `import java.nio.file.Files;`
- `import java.nio.file.Path;`
- `import java.nio.file.StandardCopyOption;`

**If imports are missing, they were added in the latest changes.**

### If Docker Build Fails

```bash
# Check Docker is running
docker ps

# View build logs
docker-compose build app 2>&1 | tail -50

# Clean everything and rebuild
docker-compose down -v
mvn clean
docker-compose build --no-cache
```

### If File Upload Doesn't Appear

**Possible causes:**
1. Docker image not rebuilt (use `--no-cache`)
2. Old JAR in Docker (rebuild without cache)
3. Browser cache (hard refresh: Ctrl+Shift+R)

**Solution:**
```bash
docker-compose down
mvn clean package -DskipTests
docker-compose build --no-cache
docker-compose up -d
```

---

## Files Modified

### Source Code
- ✅ `src/main/java/com/example/untitled/controller/BookController.java`
  - Added imports
  - Updated `importCsvTraditional()`
  - Updated `importCsvVirtualThreads()`

### Build Scripts
- ✅ `build_and_deploy.sh` (NEW) - Automated build and deploy script

### Documentation
- ✅ `CSV_UPLOAD_IMPLEMENTATION.md` - Implementation details
- ✅ `CSV_UPLOAD_SUMMARY.md` - Quick summary
- ✅ This file: `BUILD_DEPLOY_GUIDE.md` - Build and deploy guide

---

## Verification Checklist

After deployment, verify:

- [ ] Maven build successful (`BUILD SUCCESS`)
- [ ] JAR file created (`target/untitled-0.0.1-SNAPSHOT.jar` exists)
- [ ] Docker build successful
- [ ] Docker containers running (`docker ps`)
- [ ] Health check returns UP (`curl http://localhost:8080/health`)
- [ ] OpenAPI spec accessible (`curl http://localhost:8080/v3/api-docs`)
- [ ] Swagger UI loads (`http://localhost:8080/swagger-ui/index.html`)
- [ ] CSV import endpoints show file upload button in Swagger UI
- [ ] Can upload and import a CSV file successfully

---

## Testing the New Feature

### Create Test Data
```bash
cat > books_test.csv << 'EOF'
title,author,price
Clean Code,Robert C. Martin,45.99
Effective Java,Joshua Bloch,49.99
Design Patterns,Gang of Four,54.99
The Pragmatic Programmer,Andy Hunt,42.99
Refactoring,Martin Fowler,52.99
EOF
```

### Test Traditional Method
```bash
curl -X POST http://localhost:8080/api/books/import-csv-traditional \
  -H "Content-Type: multipart/form-data" \
  -F "file=@books_test.csv"
```

### Test Virtual Threads Method
```bash
curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads \
  -H "Content-Type: multipart/form-data" \
  -F "file=@books_test.csv"
```

### Verify Import
```bash
curl http://localhost:8080/api/books | python3 -m json.tool
```

Should show the imported books.

---

## Performance Comparison

You can now easily compare traditional vs virtual threads:

```bash
# Test with large CSV (use your actual CSV files)
time curl -X POST http://localhost:8080/api/books/import-csv-traditional \
  -H "Content-Type: multipart/form-data" \
  -F "file=@books_100000.csv"

time curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads \
  -H "Content-Type: multipart/form-data" \
  -F "file=@books_100000.csv"
```

The response will include timing from the server side.

---

## Summary

### ✅ Code Changes
- Imports added
- Methods updated to accept MultipartFile
- File validation added
- Temporary file handling implemented
- Error handling improved

### ✅ Ready to Deploy
- Build script created
- Documentation complete
- Testing steps provided

### 📋 Next Actions
1. Run `./build_and_deploy.sh`
2. Wait for deployment to complete
3. Test file upload in Swagger UI
4. Verify with test CSV files

---

**Status: Ready to build and deploy!** 🚀

Run: `./build_and_deploy.sh`

