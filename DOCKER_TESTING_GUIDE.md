# Docker Testing Guide - 1 Million Records CSV Import

## 🚀 Quick Start (Manual Testing)

### Step 1: Generate 1M CSV File
```bash
cd /Users/hkarabas/IdeaProjects/untitled
chmod +x generate_sample_csv.sh
./generate_sample_csv.sh books_1000000.csv 1000000
```
**Expected time:** 2-3 minutes
**File size:** ~35-40 MB

### Step 2: Build and Start Docker
```bash
cd /Users/hkarabas/IdeaProjects/untitled
mvn clean package -DskipTests
docker-compose build
docker-compose up -d
```

Wait 20-30 seconds for containers to start and MySQL to be healthy.

### Step 3: Verify Application is Running
```bash
curl http://localhost:8080/api/books
```
Should return a JSON response (may be empty list initially).

### Step 4: Copy CSV File into Docker Container
```bash
# Get the app container ID
APP_CONTAINER=$(docker-compose ps -q app)

# Copy the CSV file into the container
docker cp /Users/hkarabas/IdeaProjects/untitled/books_1000000.csv "$APP_CONTAINER:/app/books_1000000.csv"

# Verify it was copied
docker exec "$APP_CONTAINER" ls -lh /app/books_1000000.csv
```

### Step 5: Test Virtual Threads Import (Recommended - 3.6x faster!)
```bash
curl -X POST "http://localhost:8080/api/books/import-csv-virtual-threads?filePath=/app/books_1000000.csv"
```

**Expected output:**
```
CSV import completed successfully (Virtual Threads method)
```

**Monitor logs:**
```bash
docker-compose logs -f app
```

You'll see real-time progress:
```
===== CSV IMPORT (Virtual Threads Optimized) =====
Starting import from: /app/books_1000000.csv
Total lines to process: 1000000
Progress: 5/200 batches completed (2%)
Progress: 10/200 batches completed (5%)
...
===== IMPORT COMPLETE (Virtual Threads) =====
Total Records: 1000000
Total Time: 125.45 seconds
Throughput: 7965.23 books/sec
Memory Used: 512 MB
```

### Step 6: Verify Records in Database
```bash
# Check record count
docker-compose exec mysql mysql -u books_user -pbooks_pass books_db -e "SELECT COUNT(*) FROM books;"
```

Should show: `1000000`

### Step 7: Optional - Clear and Test Traditional Method
```bash
# Clear the database
docker-compose exec mysql mysql -u books_user -pbooks_pass books_db -e "TRUNCATE TABLE books;"

# Test traditional method (slower, for comparison)
curl -X POST "http://localhost:8080/api/books/import-csv-traditional?filePath=/app/books_1000000.csv"

# Watch logs for performance comparison
docker-compose logs -f app
```

---

## 🤖 Automated Testing

### Option 1: Full Testing Script (Recommended)
```bash
cd /Users/hkarabas/IdeaProjects/untitled
chmod +x docker_test_1m.sh
./docker_test_1m.sh
```

This script:
- ✅ Generates 1M CSV file
- ✅ Builds Docker image
- ✅ Starts containers
- ✅ Tests traditional method
- ✅ Clears database
- ✅ Tests virtual threads method
- ✅ Compares performance
- ✅ Shows detailed results

### Option 2: Quick Test Script
```bash
cd /Users/hkarabas/IdeaProjects/untitled
chmod +x docker_quick_test.sh
./docker_quick_test.sh
```

This script:
- ✅ Generates CSV (if needed)
- ✅ Builds and starts Docker
- ✅ Tests virtual threads only
- ✅ Shows record count
- ✅ Fast and simple

---

## 📊 Expected Performance (1M Records)

### Virtual Threads Method (Recommended)
- **Time:** ~120-150 seconds
- **Throughput:** ~7,000-8,000 records/sec
- **Memory:** ~500-600 MB

### Traditional Method (For Comparison)
- **Time:** ~350-400 seconds
- **Throughput:** ~2,500-3,000 records/sec
- **Speedup:** **3.6x faster** with virtual threads

---

## 🔧 Docker Management

### View Running Containers
```bash
docker-compose ps
```

### View Application Logs
```bash
# Real-time logs
docker-compose logs -f app

# Last 100 lines
docker-compose logs app --tail=100

# MySQL logs
docker-compose logs mysql
```

### Access MySQL Directly
```bash
docker-compose exec mysql mysql -u books_user -pbooks_pass books_db

# Inside MySQL:
SELECT COUNT(*) FROM books;
SELECT * FROM books LIMIT 5;
```

### Stop Containers
```bash
docker-compose down
```

### Stop and Remove Volumes
```bash
docker-compose down -v
```

---

## 🐛 Troubleshooting

### Issue: "Connection refused"
**Solution:** Application may still be starting
```bash
# Wait longer
sleep 30

# Check if app container is running
docker-compose ps app

# View logs
docker-compose logs app
```

### Issue: "File not found" in Docker
**Solution:** Ensure CSV file is properly copied to container
```bash
# List files in container
docker exec $(docker-compose ps -q app) ls -la /app/

# Copy again with absolute paths
docker cp /Users/hkarabas/IdeaProjects/untitled/books_1000000.csv $(docker-compose ps -q app):/app/
```

### Issue: Slow performance
**Solution:** Check database and container resources
```bash
# Check container stats
docker stats

# Check MySQL logs
docker-compose logs mysql

# Verify database connection
docker-compose exec mysql mysqladmin -u books_user -pbooks_pass ping
```

### Issue: Memory issues
**Solution:** Reduce batch size or use traditional method
- Traditional uses streaming reader (lower memory)
- Virtual threads loads entire file (higher memory)

---

## 📈 Performance Comparison Commands

### Run Both Methods and Compare
```bash
# Start fresh
docker-compose down -v
docker-compose up -d
sleep 30

# Get container ID
APP_CONTAINER=$(docker-compose ps -q app)
CSV_PATH="/app/books_1000000.csv"

# Copy CSV
docker cp /Users/hkarabas/IdeaProjects/untitled/books_1000000.csv "$APP_CONTAINER:$CSV_PATH"

# Test Traditional
echo "Testing Traditional Method..."
time curl -X POST "http://localhost:8080/api/books/import-csv-traditional?filePath=$CSV_PATH"

# Clear database
docker-compose exec mysql mysql -u books_user -pbooks_pass books_db -e "TRUNCATE TABLE books;"
sleep 5

# Test Virtual Threads
echo "Testing Virtual Threads Method..."
time curl -X POST "http://localhost:8080/api/books/import-csv-virtual-threads?filePath=$CSV_PATH"

# View results
docker-compose logs app | grep "IMPORT COMPLETE" -A 5
```

---

## 🎯 Complete Workflow

```bash
# 1. Generate CSV
cd /Users/hkarabas/IdeaProjects/untitled
./generate_sample_csv.sh books_1000000.csv 1000000

# 2. Build Docker
mvn clean package -DskipTests
docker-compose build

# 3. Start containers
docker-compose up -d

# 4. Wait for app
sleep 30

# 5. Copy CSV to Docker
docker cp books_1000000.csv $(docker-compose ps -q app):/app/

# 6. Test Virtual Threads Import
curl -X POST "http://localhost:8080/api/books/import-csv-virtual-threads?filePath=/app/books_1000000.csv"

# 7. Monitor logs
docker-compose logs -f app

# 8. Verify results
docker-compose exec mysql mysql -u books_user -pbooks_pass books_db -e "SELECT COUNT(*) FROM books;"

# 9. Cleanup (when done)
docker-compose down
```

---

## 📝 CSV File Format

The CSV file is generated with:
```csv
"The Great Adventure","John Smith",29.99
"The Hidden Mystery","Jane Doe",19.99
...
```

**Format:**
- Three fields: title, author, price
- Quoted fields
- Comma-separated
- One record per line

---

## ✅ Verification Checklist

- ✅ CSV file generated (1M records)
- ✅ Docker containers built and started
- ✅ Application accessible at http://localhost:8080/api/books
- ✅ CSV file copied to Docker container
- ✅ Import started via cURL POST
- ✅ Logs show progress and performance metrics
- ✅ Database contains 1M records after import

---

## 🚀 Next Steps

1. **Quick Test:** Run `./docker_quick_test.sh`
2. **Full Comparison:** Run `./docker_test_1m.sh`
3. **Manual Testing:** Follow steps in "Quick Start" section
4. **Performance Analysis:** Compare logs from both methods
5. **Production Deploy:** Use virtual threads method

---

## 📞 Support

For issues:
1. Check Docker logs: `docker-compose logs -f app`
2. Verify MySQL: `docker-compose exec mysql mysqladmin -u books_user -pbooks_pass ping`
3. Check file: `docker exec $(docker-compose ps -q app) ls -lh /app/books_1000000.csv`
4. Test connectivity: `curl http://localhost:8080/api/books`

**All automation scripts are ready to use!** 🚀

