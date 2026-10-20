# Docker Testing - Command Reference

## 🚀 One-Command Quick Start

```bash
# Complete test with 1M records (fully automated)
cd /Users/hkarabas/IdeaProjects/untitled && \
chmod +x docker_test_1m.sh && \
./docker_test_1m.sh
```

---

## 📋 Step-by-Step Manual Testing

### 1️⃣ Generate 1 Million Records CSV
```bash
cd /Users/hkarabas/IdeaProjects/untitled
chmod +x generate_sample_csv.sh
./generate_sample_csv.sh books_1000000.csv 1000000
```
**Output:** CSV file (~35-40 MB) in project directory
**Time:** 2-3 minutes

### 2️⃣ Build Docker Image
```bash
cd /Users/hkarabas/IdeaProjects/untitled
mvn clean package -DskipTests
docker-compose build
```

### 3️⃣ Start Docker Containers
```bash
docker-compose up -d
```
**Wait:** 20-30 seconds for containers to fully start

### 4️⃣ Verify Application is Running
```bash
curl http://localhost:8080/api/books
```
**Expected:** JSON response (empty list or existing records)

### 5️⃣ Copy CSV File into Docker
```bash
# Store container ID
APP_CONTAINER=$(docker-compose ps -q app)

# Copy CSV file
docker cp /Users/hkarabas/IdeaProjects/untitled/books_1000000.csv "$APP_CONTAINER:/app/books_1000000.csv"

# Verify copy
docker exec "$APP_CONTAINER" ls -lh /app/books_1000000.csv
```

### 6️⃣ Test Virtual Threads Import (Recommended)
```bash
curl -X POST "http://localhost:8080/api/books/import-csv-virtual-threads?filePath=/app/books_1000000.csv"
```

**Watch logs in real-time:**
```bash
docker-compose logs -f app
```

**You'll see:**
```
===== CSV IMPORT (Virtual Threads Optimized) =====
Starting import from: /app/books_1000000.csv
Total lines to process: 1000000
Progress: 5/200 batches completed (2%)
Progress: 10/200 batches completed (5%)
...
Total Time: 125.45 seconds
Throughput: 7965.23 books/sec
```

### 7️⃣ Verify Records in Database
```bash
docker-compose exec mysql mysql -u books_user -pbooks_pass books_db -e "SELECT COUNT(*) FROM books;"
```
**Expected:** `1000000`

---

## 🎯 Performance Comparison

### Clear Database
```bash
docker-compose exec mysql mysql -u books_user -pbooks_pass books_db -e "TRUNCATE TABLE books;"
```

### Test Traditional Method (Slower)
```bash
curl -X POST "http://localhost:8080/api/books/import-csv-traditional?filePath=/app/books_1000000.csv"
```

### Compare Metrics in Logs
```bash
docker-compose logs app | grep "IMPORT COMPLETE" -A 5
```

---

## 🐳 Docker Management Commands

### View Container Status
```bash
docker-compose ps
```

### View Real-Time Logs
```bash
docker-compose logs -f app
```

### View Last 50 Lines
```bash
docker-compose logs app --tail=50
```

### Access MySQL CLI
```bash
docker-compose exec mysql mysql -u books_user -pbooks_pass books_db
```

### Check MySQL Health
```bash
docker-compose exec mysql mysqladmin -u books_user -pbooks_pass ping
```

### Stop Containers
```bash
docker-compose down
```

### Stop and Remove All Data
```bash
docker-compose down -v
```

### Restart Containers
```bash
docker-compose restart
```

---

## 📊 Quick Performance Test Script

```bash
#!/bin/bash
cd /Users/hkarabas/IdeaProjects/untitled

# Generate CSV (if needed)
if [ ! -f "books_1000000.csv" ]; then
    ./generate_sample_csv.sh books_1000000.csv 1000000
fi

# Build and start
mvn clean package -DskipTests -q 2>/dev/null || true
docker-compose build -q
docker-compose up -d
sleep 30

# Copy CSV
docker cp books_1000000.csv $(docker-compose ps -q app):/app/

# Test and time
echo "🚀 Virtual Threads Test (1M records):"
time curl -X POST "http://localhost:8080/api/books/import-csv-virtual-threads?filePath=/app/books_1000000.csv"

# Verify
echo ""
echo "📊 Record count:"
docker-compose exec mysql mysql -u books_user -pbooks_pass books_db -e "SELECT COUNT(*) FROM books;"

# View key metrics
echo ""
echo "📈 Performance metrics from logs:"
docker-compose logs app | grep -E "Total Time|Throughput|Memory Used"
```

---

## 🔄 Complete Test Workflow

```bash
#!/bin/bash
cd /Users/hkarabas/IdeaProjects/untitled

# 1. Generate CSV
echo "📝 Generating 1M records..."
./generate_sample_csv.sh books_1000000.csv 1000000

# 2. Build Docker
echo "🐳 Building Docker image..."
mvn clean package -DskipTests -q 2>/dev/null || true
docker-compose build -q

# 3. Start Docker
echo "🚀 Starting Docker containers..."
docker-compose down -v 2>/dev/null || true
docker-compose up -d
sleep 30

# 4. Copy CSV
echo "📂 Copying CSV to Docker..."
APP_CONTAINER=$(docker-compose ps -q app)
docker cp books_1000000.csv "$APP_CONTAINER:/app/books_1000000.csv"

# 5. Test Virtual Threads
echo "⏱️  Testing Virtual Threads method..."
VIRTUAL_START=$(date +%s)
curl -s -X POST "http://localhost:8080/api/books/import-csv-virtual-threads?filePath=/app/books_1000000.csv" > /dev/null
VIRTUAL_END=$(date +%s)
VIRTUAL_TIME=$((VIRTUAL_END - VIRTUAL_START))

# 6. Verify
RECORD_COUNT=$(docker-compose exec -T mysql mysql -u books_user -pbooks_pass books_db -e "SELECT COUNT(*) FROM books;" 2>/dev/null | tail -1)

# 7. Results
echo ""
echo "========================================"
echo "✨ Results:"
echo "========================================"
echo "Time: $VIRTUAL_TIME seconds"
echo "Records: $RECORD_COUNT"
echo "Throughput: $((1000000 / VIRTUAL_TIME)) records/sec"
echo ""
echo "📋 Full metrics:"
docker-compose logs app | tail -20
```

---

## 🔐 Database Credentials

```
Username: books_user
Password: books_pass
Database: books_db
Port: 3306 (in Docker)
```

---

## 🎯 Test Variations

### Test with Different Record Counts

**100K records:**
```bash
./generate_sample_csv.sh books_100000.csv 100000
docker cp books_100000.csv $(docker-compose ps -q app):/app/
curl -X POST "http://localhost:8080/api/books/import-csv-virtual-threads?filePath=/app/books_100000.csv"
```

**500K records:**
```bash
./generate_sample_csv.sh books_500000.csv 500000
docker cp books_500000.csv $(docker-compose ps -q app):/app/
curl -X POST "http://localhost:8080/api/books/import-csv-virtual-threads?filePath=/app/books_500000.csv"
```

**5M records:**
```bash
./generate_sample_csv.sh books_5000000.csv 5000000
docker cp books_5000000.csv $(docker-compose ps -q app):/app/
curl -X POST "http://localhost:8080/api/books/import-csv-virtual-threads?filePath=/app/books_5000000.csv"
```

---

## 📈 Performance Metrics to Monitor

**From Application Logs:**
- Total Records processed
- Total Time in seconds
- Throughput (books/sec)
- Memory Used (MB)

**From Docker:**
- Container CPU usage: `docker stats`
- Memory usage: `docker stats`
- Disk I/O: `docker stats`

**From Database:**
- Record count: `SELECT COUNT(*) FROM books;`
- Index status: `SELECT * FROM information_schema.STATISTICS WHERE TABLE_NAME='books';`
- Table size: `SELECT * FROM information_schema.TABLES WHERE TABLE_NAME='books'\G`

---

## ✅ Success Checklist

- ✅ CSV file generated (1M records)
- ✅ Docker containers started
- ✅ Application responding to requests
- ✅ CSV file copied into Docker
- ✅ cURL POST request successful
- ✅ Logs show progress updates
- ✅ Import completed without errors
- ✅ Database contains 1M records
- ✅ Performance metrics logged
- ✅ Virtual threads faster than traditional

---

## 🚀 Recommended Workflow

1. **First time setup:** Follow "Step-by-Step Manual Testing"
2. **Subsequent tests:** Use `docker_quick_test.sh`
3. **Full comparison:** Use `docker_test_1m.sh`
4. **Quick metrics:** Run performance test script

**Start here:** `cd /Users/hkarabas/IdeaProjects/untitled && ./docker_quick_test.sh`

