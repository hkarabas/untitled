#!/bin/bash

# Quick Docker Test Script - 1M Records
# Simple script to test CSV import in Docker environment

set -e

PROJECT_DIR="/Users/hkarabas/IdeaProjects/untitled"
CSV_SIZE="1000000"
CSV_FILE="$PROJECT_DIR/books_${CSV_SIZE}.csv"
BASE_URL="http://localhost:8080/api/books"
DOCKER_CSV_PATH="/app/books_${CSV_SIZE}.csv"

echo "========================================"
echo "Docker Quick Test - 1,000,000 Records"
echo "========================================"
echo ""

# Check if CSV exists
if [ ! -f "$CSV_FILE" ]; then
    echo "📝 Generating 1M CSV file (this takes 2-3 minutes)..."
    cd "$PROJECT_DIR"
    chmod +x generate_sample_csv.sh
    ./generate_sample_csv.sh "books_${CSV_SIZE}.csv" "$CSV_SIZE"
fi

FILE_SIZE=$(du -h "$CSV_FILE" | cut -f1)
echo "✅ CSV file ready: $CSV_FILE ($FILE_SIZE)"
echo ""

# Build and start Docker
echo "🐳 Building and starting Docker containers..."
cd "$PROJECT_DIR"
mvn clean package -DskipTests -q 2>/dev/null || true
docker-compose build -q
docker-compose down 2>/dev/null || true
docker-compose up -d

# Wait for app
echo "⏳ Waiting for application to start..."
sleep 20
for i in {1..30}; do
    if curl -s "$BASE_URL" > /dev/null 2>&1; then
        echo "✅ Application ready"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

# Copy CSV to container
echo ""
echo "📂 Copying CSV file to Docker container..."
APP_CONTAINER=$(docker-compose ps -q app)
docker cp "$CSV_FILE" "$APP_CONTAINER:$DOCKER_CSV_PATH"
echo "✅ File copied"
echo ""

# Test Virtual Threads (recommended - faster)
echo "🚀 Testing Virtual Threads Method (recommended)..."
echo "POST $BASE_URL/import-csv-virtual-threads?filePath=$DOCKER_CSV_PATH"
echo ""

START=$(date +%s)
curl -X POST "$BASE_URL/import-csv-virtual-threads?filePath=$DOCKER_CSV_PATH"
END=$(date +%s)
DURATION=$((END - START))

echo ""
echo "✅ Virtual Threads completed in $DURATION seconds"
echo "Throughput: $(echo "scale=2; 1000000 / $DURATION" | bc) records/sec"
echo ""

# Check database
echo "📊 Verifying records in database..."
COUNT=$(docker-compose exec -T mysql mysql -u books_user -pbooks_pass books_db -e "SELECT COUNT(*) FROM books;" 2>/dev/null | tail -1)
echo "Total records: $COUNT"
echo ""

echo "========================================"
echo "✨ Test Complete!"
echo "========================================"
echo ""
echo "To stop containers: docker-compose down"
echo "To view logs: docker-compose logs -f app"
echo ""

