#!/bin/bash

echo "=========================================="
echo "Building and Deploying Books API"
echo "=========================================="
echo ""

# Step 1: Clean and build with Maven
echo "Step 1: Building with Maven..."
cd /Users/hkarabas/IdeaProjects/untitled
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ Maven build successful"
    ls -lh target/untitled-0.0.1-SNAPSHOT.jar
else
    echo "❌ Maven build failed"
    exit 1
fi

echo ""

# Step 2: Stop existing Docker containers
echo "Step 2: Stopping existing Docker containers..."
docker-compose down

echo ""

# Step 3: Rebuild Docker images (without cache)
echo "Step 3: Rebuilding Docker images..."
docker-compose build --no-cache app

if [ $? -eq 0 ]; then
    echo "✅ Docker build successful"
else
    echo "❌ Docker build failed"
    exit 1
fi

echo ""

# Step 4: Start Docker containers
echo "Step 4: Starting Docker containers..."
docker-compose up -d

if [ $? -eq 0 ]; then
    echo "✅ Docker containers started"
else
    echo "❌ Failed to start Docker containers"
    exit 1
fi

echo ""

# Step 5: Wait for application to start
echo "Step 5: Waiting for application to start (15 seconds)..."
sleep 15

echo ""

# Step 6: Verify application is running
echo "Step 6: Verifying application..."
echo ""

# Check health endpoint
echo "Checking health endpoint..."
health_response=$(curl -s http://localhost:8080/health)
if echo "$health_response" | grep -q "UP"; then
    echo "✅ Health check passed"
    echo "   Response: $health_response"
else
    echo "❌ Health check failed"
    echo "   Response: $health_response"
fi

echo ""

# Check OpenAPI endpoint
echo "Checking OpenAPI endpoint..."
openapi_check=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/v3/api-docs)
if [ "$openapi_check" = "200" ]; then
    echo "✅ OpenAPI endpoint accessible"
else
    echo "❌ OpenAPI endpoint not accessible (HTTP $openapi_check)"
fi

echo ""

# Check Swagger UI
echo "Checking Swagger UI..."
swagger_check=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui/index.html)
if [ "$swagger_check" = "200" ]; then
    echo "✅ Swagger UI accessible"
else
    echo "❌ Swagger UI not accessible (HTTP $swagger_check)"
fi

echo ""
echo "=========================================="
echo "Deployment Complete!"
echo "=========================================="
echo ""
echo "Access your API:"
echo "  • Swagger UI:     http://localhost:8080/swagger-ui/index.html"
echo "  • OpenAPI Spec:   http://localhost:8080/v3/api-docs"
echo "  • API Base:       http://localhost:8080/api/books"
echo "  • Health Check:   http://localhost:8080/health"
echo ""
echo "CSV Import endpoints now support file upload!"
echo "  • POST /api/books/import-csv-traditional (multipart/form-data)"
echo "  • POST /api/books/import-csv-virtual-threads (multipart/form-data)"
echo ""
echo "View logs: docker logs -f untitled-app-1"
echo "Stop: docker-compose down"
echo ""

