#!/bin/bash

echo "========================================"
echo "OpenAPI / Swagger UI Test Script"
echo "========================================"
echo ""

# Wait for application to start
echo "Waiting for application to start..."
sleep 5

# Test 1: Check if application is running
echo "Test 1: Checking if application is running..."
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/health)
if [ "$response" = "200" ]; then
    echo "✅ Application is running (HTTP $response)"
else
    echo "❌ Application not responding properly (HTTP $response)"
fi
echo ""

# Test 2: Check OpenAPI specification (JSON)
echo "Test 2: Checking OpenAPI specification (JSON)..."
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/v3/api-docs)
if [ "$response" = "200" ]; then
    echo "✅ OpenAPI spec accessible (HTTP $response)"
    echo "   URL: http://localhost:8080/v3/api-docs"

    # Get OpenAPI version
    version=$(curl -s http://localhost:8080/v3/api-docs | grep -o '"openapi":"[^"]*"' | cut -d'"' -f4)
    if [ ! -z "$version" ]; then
        echo "   OpenAPI Version: $version"
    fi

    # Get API title
    title=$(curl -s http://localhost:8080/v3/api-docs | grep -o '"title":"[^"]*"' | head -1 | cut -d'"' -f4)
    if [ ! -z "$title" ]; then
        echo "   API Title: $title"
    fi
else
    echo "❌ OpenAPI spec not accessible (HTTP $response)"
fi
echo ""

# Test 3: Check OpenAPI specification (YAML)
echo "Test 3: Checking OpenAPI specification (YAML)..."
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/v3/api-docs.yaml)
if [ "$response" = "200" ]; then
    echo "✅ OpenAPI spec (YAML) accessible (HTTP $response)"
    echo "   URL: http://localhost:8080/v3/api-docs.yaml"
else
    echo "❌ OpenAPI spec (YAML) not accessible (HTTP $response)"
fi
echo ""

# Test 4: Check Swagger UI
echo "Test 4: Checking Swagger UI..."
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui/index.html)
if [ "$response" = "200" ]; then
    echo "✅ Swagger UI accessible (HTTP $response)"
    echo "   URL: http://localhost:8080/swagger-ui/index.html"
else
    echo "❌ Swagger UI not accessible (HTTP $response)"
fi
echo ""

# Test 5: Check API endpoints
echo "Test 5: Checking API endpoints..."
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/books)
if [ "$response" = "200" ]; then
    echo "✅ API endpoint accessible (HTTP $response)"
    echo "   URL: http://localhost:8080/api/books"
else
    echo "❌ API endpoint not accessible (HTTP $response)"
fi
echo ""

# Test 6: Check root path
echo "Test 6: Checking root path..."
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/)
if [ "$response" = "200" ]; then
    echo "✅ Root path accessible (HTTP $response)"
    echo "   URL: http://localhost:8080/"
else
    echo "❌ Root path not accessible (HTTP $response)"
fi
echo ""

# Summary
echo "========================================"
echo "Summary"
echo "========================================"
echo ""
echo "Access your API documentation at:"
echo "  • Swagger UI:     http://localhost:8080/swagger-ui/index.html"
echo "  • OpenAPI (JSON): http://localhost:8080/v3/api-docs"
echo "  • OpenAPI (YAML): http://localhost:8080/v3/api-docs.yaml"
echo "  • API Base:       http://localhost:8080/api/books"
echo "  • Root/Help:      http://localhost:8080/"
echo ""
echo "Debug Port: localhost:5005"
echo ""

# Display OpenAPI info
echo "OpenAPI Specification Details:"
echo "------------------------------"
curl -s http://localhost:8080/v3/api-docs | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f\"OpenAPI Version: {data.get('openapi', 'N/A')}\")
    info = data.get('info', {})
    print(f\"Title: {info.get('title', 'N/A')}\")
    print(f\"Version: {info.get('version', 'N/A')}\")
    print(f\"Description: {info.get('description', 'N/A')}\")
    print(f\"\\nEndpoints: {len(data.get('paths', {}))} paths documented\")
except:
    print('Could not parse OpenAPI spec')
" 2>/dev/null || echo "Run 'curl http://localhost:8080/v3/api-docs | jq .' to see full spec"

echo ""
echo "========================================"
echo "✅ All tests complete!"
echo "========================================"

