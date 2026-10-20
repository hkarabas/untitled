#!/bin/bash

# Test script for Parallel Stream CSV Import endpoint
# This script tests the new /api/books/import-csv-parallel-stream endpoint

echo "======================================"
echo "Parallel Stream CSV Import Test"
echo "======================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
API_URL="http://localhost:8080/api/books"
TEST_FILE="books_100000.csv"

# Function to print colored messages
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Step 1: Check if application is running
echo "Step 1: Checking if application is running..."
if curl -s -f -o /dev/null "$API_URL" 2>/dev/null; then
    print_success "Application is running at $API_URL"
else
    print_error "Application is not running or not responding"
    echo "Please start the application with: docker-compose up -d"
    exit 1
fi
echo ""

# Step 2: Check if test file exists
echo "Step 2: Checking for test CSV file..."
if [ -f "$TEST_FILE" ]; then
    print_success "Test file found: $TEST_FILE"
    FILE_SIZE=$(ls -lh "$TEST_FILE" | awk '{print $5}')
    LINE_COUNT=$(wc -l < "$TEST_FILE")
    print_info "File size: $FILE_SIZE"
    print_info "Line count: $LINE_COUNT"
else
    print_error "Test file not found: $TEST_FILE"
    echo "Generate it with: ./generate_sample_csv.sh 100000"
    exit 1
fi
echo ""

# Step 3: Clear existing books (optional)
echo "Step 3: Checking current book count..."
BOOK_COUNT=$(curl -s "$API_URL" | jq 'length' 2>/dev/null || echo "0")
print_info "Current books in database: $BOOK_COUNT"
echo ""

# Step 4: Test Traditional method
echo "Step 4: Testing Traditional CSV Import..."
print_info "Uploading $TEST_FILE using Traditional method..."
TRADITIONAL_START=$(date +%s)
TRADITIONAL_RESPONSE=$(curl -s -X POST "$API_URL/import-csv-traditional" \
    -F "file=@$TEST_FILE" \
    -w "\n%{http_code}" 2>/dev/null)
TRADITIONAL_END=$(date +%s)
TRADITIONAL_HTTP_CODE=$(echo "$TRADITIONAL_RESPONSE" | tail -n 1)
TRADITIONAL_BODY=$(echo "$TRADITIONAL_RESPONSE" | head -n -1)
TRADITIONAL_DURATION=$((TRADITIONAL_END - TRADITIONAL_START))

if [ "$TRADITIONAL_HTTP_CODE" = "200" ]; then
    print_success "Traditional import completed (HTTP $TRADITIONAL_HTTP_CODE)"
    print_info "Response: $TRADITIONAL_BODY"
    print_info "Total time (including network): ${TRADITIONAL_DURATION}s"
else
    print_error "Traditional import failed (HTTP $TRADITIONAL_HTTP_CODE)"
    echo "Response: $TRADITIONAL_BODY"
fi
echo ""

sleep 2

# Step 5: Test Virtual Threads method
echo "Step 5: Testing Virtual Threads CSV Import..."
print_info "Uploading $TEST_FILE using Virtual Threads method..."
VIRTUAL_START=$(date +%s)
VIRTUAL_RESPONSE=$(curl -s -X POST "$API_URL/import-csv-virtual-threads" \
    -F "file=@$TEST_FILE" \
    -w "\n%{http_code}" 2>/dev/null)
VIRTUAL_END=$(date +%s)
VIRTUAL_HTTP_CODE=$(echo "$VIRTUAL_RESPONSE" | tail -n 1)
VIRTUAL_BODY=$(echo "$VIRTUAL_RESPONSE" | head -n -1)
VIRTUAL_DURATION=$((VIRTUAL_END - VIRTUAL_START))

if [ "$VIRTUAL_HTTP_CODE" = "200" ]; then
    print_success "Virtual Threads import completed (HTTP $VIRTUAL_HTTP_CODE)"
    print_info "Response: $VIRTUAL_BODY"
    print_info "Total time (including network): ${VIRTUAL_DURATION}s"
else
    print_error "Virtual Threads import failed (HTTP $VIRTUAL_HTTP_CODE)"
    echo "Response: $VIRTUAL_BODY"
fi
echo ""

sleep 2

# Step 6: Test Parallel Stream method (NEW!)
echo "Step 6: Testing Parallel Stream CSV Import..."
print_info "Uploading $TEST_FILE using Parallel Stream method..."
PARALLEL_STREAM_START=$(date +%s)
PARALLEL_STREAM_RESPONSE=$(curl -s -X POST "$API_URL/import-csv-parallel-stream" \
    -F "file=@$TEST_FILE" \
    -w "\n%{http_code}" 2>/dev/null)
PARALLEL_STREAM_END=$(date +%s)
PARALLEL_STREAM_HTTP_CODE=$(echo "$PARALLEL_STREAM_RESPONSE" | tail -n 1)
PARALLEL_STREAM_BODY=$(echo "$PARALLEL_STREAM_RESPONSE" | head -n -1)
PARALLEL_STREAM_DURATION=$((PARALLEL_STREAM_END - PARALLEL_STREAM_START))

if [ "$PARALLEL_STREAM_HTTP_CODE" = "200" ]; then
    print_success "Parallel Stream import completed (HTTP $PARALLEL_STREAM_HTTP_CODE)"
    print_info "Response: $PARALLEL_STREAM_BODY"
    print_info "Total time (including network): ${PARALLEL_STREAM_DURATION}s"
else
    print_error "Parallel Stream import failed (HTTP $PARALLEL_STREAM_HTTP_CODE)"
    echo "Response: $PARALLEL_STREAM_BODY"
fi
echo ""

sleep 2

# Step 7: Test Fire-and-Forget method
echo "Step 7: Testing Fire-and-Forget Parallel CSV Import..."
print_info "Uploading $TEST_FILE using Fire-and-Forget method..."
FIRE_FORGET_START=$(date +%s)
FIRE_FORGET_RESPONSE=$(curl -s -X POST "$API_URL/import-csv-parallel-fire-and-forget" \
    -F "file=@$TEST_FILE" \
    -w "\n%{http_code}" 2>/dev/null)
FIRE_FORGET_END=$(date +%s)
FIRE_FORGET_HTTP_CODE=$(echo "$FIRE_FORGET_RESPONSE" | tail -n 1)
FIRE_FORGET_BODY=$(echo "$FIRE_FORGET_RESPONSE" | head -n -1)
FIRE_FORGET_DURATION=$((FIRE_FORGET_END - FIRE_FORGET_START))

if [ "$FIRE_FORGET_HTTP_CODE" = "202" ]; then
    print_success "Fire-and-Forget import accepted (HTTP $FIRE_FORGET_HTTP_CODE)"
    print_info "Response: $FIRE_FORGET_BODY"
    print_info "Response time: ${FIRE_FORGET_DURATION}s (async, processing continues in background)"
else
    print_error "Fire-and-Forget import failed (HTTP $FIRE_FORGET_HTTP_CODE)"
    echo "Response: $FIRE_FORGET_BODY"
fi
echo ""

# Step 8: Summary
echo "======================================"
echo "Performance Comparison Summary"
echo "======================================"
echo ""
echo "Method                     | HTTP Code | Response Time"
echo "---------------------------|-----------|---------------"
echo "Traditional                | $TRADITIONAL_HTTP_CODE       | ${TRADITIONAL_DURATION}s"
echo "Virtual Threads            | $VIRTUAL_HTTP_CODE       | ${VIRTUAL_DURATION}s"
echo "Parallel Stream (NEW)      | $PARALLEL_STREAM_HTTP_CODE       | ${PARALLEL_STREAM_DURATION}s"
echo "Fire-and-Forget            | $FIRE_FORGET_HTTP_CODE       | ${FIRE_FORGET_DURATION}s (async)"
echo ""

# Step 9: Verify final book count
echo "Step 9: Verifying final book count..."
sleep 3
FINAL_COUNT=$(curl -s "$API_URL" | jq 'length' 2>/dev/null || echo "unknown")
print_info "Final books in database: $FINAL_COUNT"
echo ""

print_success "All tests completed!"
echo ""
echo "Note: Check docker-compose logs for detailed import metrics:"
echo "  docker-compose logs app | grep 'CSV IMPORT'"

