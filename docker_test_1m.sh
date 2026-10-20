#!/bin/bash

# Docker Testing Script for 1 Million Records CSV Import
# This script tests both traditional and virtual thread CSV import methods
# with Docker Compose for a realistic production environment

set -e

PROJECT_DIR="/Users/hkarabas/IdeaProjects/untitled"
TEST_CSV_SIZE="1000000"
CSV_FILE="$PROJECT_DIR/books_${TEST_CSV_SIZE}.csv"
BASE_URL="http://localhost:8080/api/books"
CONTAINER_CSV_PATH="/app/books_${TEST_CSV_SIZE}.csv"

echo "=========================================="
echo "Docker CSV Import Test - 1 Million Records"
echo "=========================================="
echo ""

# Step 1: Generate 1M record CSV file
echo "📝 Step 1: Generating 1,000,000 record CSV file..."
echo "This may take 2-3 minutes..."
echo ""

if [ ! -f "$CSV_FILE" ]; then
    cd "$PROJECT_DIR"
    chmod +x generate_sample_csv.sh
    echo "Starting CSV generation..."
    ./generate_sample_csv.sh "books_${TEST_CSV_SIZE}.csv" "$TEST_CSV_SIZE"
    echo "✅ CSV file generated: $CSV_FILE"
else
    echo "✅ CSV file already exists: $CSV_FILE"
    echo "File size: $(du -h "$CSV_FILE" | cut -f1)"
fi
echo ""

# Step 2: Build Docker image
echo "🐳 Step 2: Building Docker image..."
cd "$PROJECT_DIR"
mvn clean package -DskipTests -q
docker-compose build
echo "✅ Docker image built"
echo ""

# Step 3: Start Docker containers
echo "🚀 Step 3: Starting Docker containers..."
docker-compose up -d
echo "Waiting for MySQL to be healthy..."
sleep 10
echo "Waiting for Spring Boot application..."
sleep 15
echo "✅ Docker containers started"
echo ""

# Step 4: Verify application is running
echo "🔍 Step 4: Verifying application is running..."
MAX_RETRIES=30
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s "$BASE_URL" > /dev/null 2>&1; then
        echo "✅ Application is running at $BASE_URL"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "Waiting for application... (attempt $RETRY_COUNT/$MAX_RETRIES)"
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "❌ Application did not start in time"
    docker-compose logs app
    docker-compose down
    exit 1
fi
echo ""

# Step 5: Copy CSV file to Docker container
echo "📂 Step 5: Copying CSV file to Docker container..."
docker cp "$CSV_FILE" "$(docker-compose ps -q app):$CONTAINER_CSV_PATH"
echo "✅ CSV file copied to container: $CONTAINER_CSV_PATH"
echo ""

# Step 6: Test Traditional Method
echo "⏱️  Step 6: Testing Traditional Method (Single-threaded)..."
echo "Importing 1,000,000 records..."
echo "Endpoint: POST $BASE_URL/import-csv-traditional"
echo ""

TRADITIONAL_START=$(date +%s%N)
TRADITIONAL_RESPONSE=$(curl -s -X POST "$BASE_URL/import-csv-traditional?filePath=$CONTAINER_CSV_PATH")
TRADITIONAL_END=$(date +%s%N)
TRADITIONAL_DURATION_NS=$((TRADITIONAL_END - TRADITIONAL_START))
TRADITIONAL_DURATION=$(echo "scale=2; $TRADITIONAL_DURATION_NS / 1000000000" | bc)

echo "Response: $TRADITIONAL_RESPONSE"
echo "✅ Traditional import completed in $TRADITIONAL_DURATION seconds"
echo ""

# Step 7: Get record count after traditional import
echo "📊 Checking database record count..."
RECORD_COUNT=$(docker-compose exec -T mysql mysql -u books_user -pbooks_pass books_db -e "SELECT COUNT(*) FROM books;" 2>/dev/null | tail -1)
echo "Records in database: $RECORD_COUNT"
echo ""

# Step 8: Clear database for virtual threads test
echo "🗑️  Step 8: Clearing database for virtual threads test..."
docker-compose exec -T mysql mysql -u books_user -pbooks_pass books_db -e "TRUNCATE TABLE books;" 2>/dev/null
CLEARED_COUNT=$(docker-compose exec -T mysql mysql -u books_user -pbooks_pass books_db -e "SELECT COUNT(*) FROM books;" 2>/dev/null | tail -1)
echo "✅ Database cleared. Record count: $CLEARED_COUNT"
echo ""

# Step 9: Test Virtual Threads Method
echo "⏱️  Step 9: Testing Virtual Threads Method (Multi-threaded)..."
echo "Importing 1,000,000 records with virtual threads..."
echo "Endpoint: POST $BASE_URL/import-csv-virtual-threads"
echo ""

VIRTUAL_START=$(date +%s%N)
VIRTUAL_RESPONSE=$(curl -s -X POST "$BASE_URL/import-csv-virtual-threads?filePath=$CONTAINER_CSV_PATH")
VIRTUAL_END=$(date +%s%N)
VIRTUAL_DURATION_NS=$((VIRTUAL_END - VIRTUAL_START))
VIRTUAL_DURATION=$(echo "scale=2; $VIRTUAL_DURATION_NS / 1000000000" | bc)

echo "Response: $VIRTUAL_RESPONSE"
echo "✅ Virtual Threads import completed in $VIRTUAL_DURATION seconds"
echo ""

# Step 10: Get final record count
echo "📊 Final database record count..."
FINAL_COUNT=$(docker-compose exec -T mysql mysql -u books_user -pbooks_pass books_db -e "SELECT COUNT(*) FROM books;" 2>/dev/null | tail -1)
echo "Records in database: $FINAL_COUNT"
echo ""

# Step 11: Compare Results
echo "=========================================="
echo "📊 PERFORMANCE COMPARISON (1M Records)"
echo "=========================================="
echo ""
echo "Traditional Method (Single-threaded):"
echo "  - Time: $TRADITIONAL_DURATION seconds"
echo "  - Throughput: $(echo "scale=2; 1000000 / $TRADITIONAL_DURATION" | bc) records/sec"
echo ""
echo "Virtual Threads Method (Multi-threaded):"
echo "  - Time: $VIRTUAL_DURATION seconds"
echo "  - Throughput: $(echo "scale=2; 1000000 / $VIRTUAL_DURATION" | bc) records/sec"
echo ""

if [ "$(echo "$VIRTUAL_DURATION > 0" | bc)" -eq 1 ]; then
    SPEEDUP=$(echo "scale=2; $TRADITIONAL_DURATION / $VIRTUAL_DURATION" | bc)
    echo "🚀 Speedup: ${SPEEDUP}x faster with Virtual Threads!"
else
    echo "⚠️  Could not calculate speedup"
fi
echo ""

# Step 12: Show Docker logs
echo "📋 Application Logs (last 50 lines):"
echo "=========================================="
docker-compose logs app | tail -50
echo ""

# Step 13: Cleanup
echo "🧹 Step 13: Cleaning up Docker containers..."
echo "Keep containers running? (y/n) [default: n]"
read -r keep_running
if [ "$keep_running" != "y" ]; then
    docker-compose down
    echo "✅ Docker containers stopped and removed"
else
    echo "✅ Docker containers still running"
    echo "To stop them later, run: docker-compose down"
fi
echo ""

echo "=========================================="
echo "✨ Docker Testing Complete!"
echo "=========================================="
echo ""
echo "Summary:"
echo "- CSV Records: 1,000,000"
echo "- CSV File: $CSV_FILE"
echo "- Traditional Time: $TRADITIONAL_DURATION seconds"
echo "- Virtual Threads Time: $VIRTUAL_DURATION seconds"
if [ "$(echo "$VIRTUAL_DURATION > 0" | bc)" -eq 1 ]; then
    echo "- Speedup: ${SPEEDUP}x"
fi
echo ""
echo "For more details, check the logs above or run:"
echo "  docker-compose logs -f app"
echo ""

