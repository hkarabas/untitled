#!/bin/bash

# CSV Import Performance Testing Script
# This script helps you test both traditional and virtual thread CSV import methods

set -e

PROJECT_DIR="/Users/hkarabas/IdeaProjects/untitled"
TEST_CSV_SIZE="${1:-100000}"
CSV_FILE="$PROJECT_DIR/books_${TEST_CSV_SIZE}.csv"
BASE_URL="http://localhost:8080/api/books"

echo "========================================"
echo "CSV Import Performance Testing"
echo "========================================"
echo ""

# Step 1: Generate test CSV file
echo "📝 Step 1: Generating test CSV file with $TEST_CSV_SIZE records..."
if [ ! -f "$CSV_FILE" ]; then
    cd "$PROJECT_DIR"
    chmod +x generate_sample_csv.sh
    ./generate_sample_csv.sh "books_${TEST_CSV_SIZE}.csv" "$TEST_CSV_SIZE"
    echo "✅ CSV file generated: $CSV_FILE"
else
    echo "✅ CSV file already exists: $CSV_FILE"
fi
echo ""

# Step 2: Verify application is running
echo "🔍 Step 2: Checking if application is running..."
if ! curl -s "$BASE_URL" > /dev/null; then
    echo "❌ Application is not running at $BASE_URL"
    echo "Please start the application with: mvn spring-boot:run"
    exit 1
fi
echo "✅ Application is running"
echo ""

# Step 3: Test Traditional Method
echo "⏱️  Step 3: Testing Traditional Method (Single-threaded)..."
echo "Sending request to: $BASE_URL/import-csv-traditional"
echo ""

TRADITIONAL_START=$(date +%s%N)
curl -X POST "$BASE_URL/import-csv-traditional?filePath=$CSV_FILE" 2>/dev/null
TRADITIONAL_END=$(date +%s%N)
TRADITIONAL_DURATION=$(echo "scale=3; ($TRADITIONAL_END - $TRADITIONAL_START) / 1000000000" | bc)

echo ""
echo "✅ Traditional import completed in $TRADITIONAL_DURATION seconds"
echo ""

# Step 4: Test Virtual Threads Method
echo "⏱️  Step 4: Testing Virtual Threads Method (Multi-threaded)..."
echo "Sending request to: $BASE_URL/import-csv-virtual-threads"
echo ""

VIRTUAL_START=$(date +%s%N)
curl -X POST "$BASE_URL/import-csv-virtual-threads?filePath=$CSV_FILE" 2>/dev/null
VIRTUAL_END=$(date +%s%N)
VIRTUAL_DURATION=$(echo "scale=3; ($VIRTUAL_END - $VIRTUAL_START) / 1000000000" | bc)

echo ""
echo "✅ Virtual Threads import completed in $VIRTUAL_DURATION seconds"
echo ""

# Step 5: Compare Results
echo "========================================"
echo "📊 Performance Comparison Results"
echo "========================================"
echo ""
echo "Traditional Method:"
echo "  - Time: $TRADITIONAL_DURATION seconds"
echo ""
echo "Virtual Threads Method:"
echo "  - Time: $VIRTUAL_DURATION seconds"
echo ""

SPEEDUP=$(echo "scale=2; $TRADITIONAL_DURATION / $VIRTUAL_DURATION" | bc)
echo "🚀 Speedup: ${SPEEDUP}x faster with Virtual Threads"
echo ""

echo "========================================"
echo "✨ Testing Complete!"
echo "========================================"
echo ""
echo "Summary:"
echo "- CSV File: $CSV_FILE"
echo "- Records: $TEST_CSV_SIZE"
echo "- Traditional Time: $TRADITIONAL_DURATION seconds"
echo "- Virtual Threads Time: $VIRTUAL_DURATION seconds"
echo "- Performance Gain: ${SPEEDUP}x"
echo ""
echo "For detailed logs, check application console output"
echo ""

