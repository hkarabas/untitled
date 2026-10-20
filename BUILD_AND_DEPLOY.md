# Build and Deploy Instructions

## Step-by-Step Guide

### Prerequisites
- Docker and Docker Compose installed
- Maven 3.9.6+ installed
- Java 25 installed
- Port 8080 and 5005 available on localhost

---

## Option 1: Docker Deployment (Recommended)

### 1. Build the Project
```bash
cd /Users/hkarabas/IdeaProjects/untitled
mvn clean package -DskipTests
```

### 2. Start with Docker Compose
```bash
docker-compose up -d
```

### 3. Wait for Startup
```bash
# Wait about 15 seconds for both MySQL and Spring Boot to start
sleep 15

# Verify containers are running
docker ps
```

### 4. Access the API

**Swagger UI Documentation:**
```
http://localhost:8080/swagger-ui/index.html
```

**API Base URL:**
```
http://localhost:8080/api/books
```

**OpenAPI Specification:**
```
http://localhost:8080/v3/api-docs
```

### 5. Test an Endpoint
```bash
# Get all books
curl http://localhost:8080/api/books

# Create a book
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "price": 45.99
  }'
```

---

## Option 2: Local Development

### 1. Build the Project
```bash
cd /Users/hkarabas/IdeaProjects/untitled
mvn clean package -DskipTests
```

### 2. Start MySQL (using Docker only)
```bash
docker run --name mysql-books \
  -e MYSQL_DATABASE=books_db \
  -e MYSQL_USER=books_user \
  -e MYSQL_PASSWORD=books_pass \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -p 3306:3306 \
  -d mysql:8.0
```

### 3. Run the Application
```bash
java -jar target/untitled-0.0.1-SNAPSHOT.jar
```

### 4. Access the Application
Same URLs as Docker option above

---

## Option 3: IDE Development

### 1. Open in IntelliJ IDEA
- File → Open → Select `/Users/hkarabas/IdeaProjects/untitled`

### 2. Build with Maven
- View → Tool Windows → Maven
- Right-click Project → Run Maven Goal
- Enter: `clean package -DskipTests`

### 3. Start MySQL (Docker)
```bash
docker run --name mysql-books \
  -e MYSQL_DATABASE=books_db \
  -e MYSQL_USER=books_user \
  -e MYSQL_PASSWORD=books_pass \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -p 3306:3306 \
  -d mysql:8.0
```

### 4. Run Application
- Click the green play button next to `UntitledApplication.main()`

### 5. Connect Debugger (Optional)
- Run → Edit Configurations
- Click "+" → Remote
- Name: "Docker Debug"
- Host: localhost
- Port: 5005
- Click OK
- Click the debug button (green bug icon)

---

## Stopping the Application

### Docker
```bash
# Stop containers
docker-compose down

# Or just stop (keep data)
docker-compose stop

# Restart
docker-compose start
```

### Local Development
- Press Ctrl+C in terminal running the app
- Or use IDE stop button

---

## Troubleshooting

### Port 8080 Already in Use
```bash
# Find and kill process using port 8080
lsof -i :8080
kill -9 <PID>

# Or use different port in application.yml
server:
  port: 8081
```

### MySQL Connection Issues
```bash
# Check if MySQL is running
docker ps | grep mysql

# Check logs
docker logs untitled-mysql-1

# Restart MySQL
docker-compose down
docker-compose up -d
```

### Swagger UI Not Loading
```bash
# Check application logs
docker logs untitled-app-1

# Verify API is responding
curl http://localhost:8080/api/books

# Check OpenAPI spec generation
curl http://localhost:8080/v3/api-docs
```

### Debug Port (5005) Connection Issues
```bash
# Verify debug port is open
nc -zv localhost 5005

# If not working, check Docker logs
docker logs untitled-app-1 | grep jdwp
```

---

## Useful Commands

### View Logs
```bash
# Application logs
docker logs untitled-app-1

# Follow logs (real-time)
docker logs -f untitled-app-1

# MySQL logs
docker logs untitled-mysql-1
```

### Database Access
```bash
# Connect to MySQL
docker exec -it untitled-mysql-1 mysql -u books_user -pbooks_pass books_db

# List tables
SHOW TABLES;

# Count books
SELECT COUNT(*) FROM books;

# Exit
EXIT;
```

### Clean Build
```bash
# Remove old build
rm -rf target/

# Full rebuild
mvn clean package -DskipTests
```

### Health Check
```bash
# API health
curl http://localhost:8080/api/books

# Swagger spec
curl http://localhost:8080/v3/api-docs | jq '.info'

# Debug port
nc -zv localhost 5005
```

---

## Performance Notes

### First Build
- Initial Maven build downloads dependencies: ~1-2 minutes
- Subsequent builds are faster: ~30-60 seconds

### First Docker Start
- Image build: 1-2 minutes
- Container startup: 10-15 seconds
- Total: 2-3 minutes

### Subsequent Starts
- Just container startup: 10-15 seconds

### Application Startup
- With 30 connection pool: 1-2 seconds
- Database schema creation: included in startup

---

## Environment Variables

### Docker Compose Configuration
```yaml
SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/books_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME: books_user
SPRING_DATASOURCE_PASSWORD: books_pass
```

### Local Development
Create `src/main/resources/application-local.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/books_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: books_user
    password: books_pass
```

Run with profile:
```bash
java -Dspring.profiles.active=local -jar target/untitled-0.0.1-SNAPSHOT.jar
```

---

## Production Checklist

- [ ] Build JAR without tests: `mvn clean package -DskipTests`
- [ ] Verify Swagger disabled or secured in production
- [ ] Update API info in `OpenApiConfig.java`
- [ ] Use environment variables for database credentials
- [ ] Enable HTTPS/TLS for API endpoints
- [ ] Set up proper logging
- [ ] Configure database backups
- [ ] Set resource limits in Docker
- [ ] Monitor application health
- [ ] Set up CI/CD pipeline

---

## Quick Start (Copy & Paste)

```bash
# Build
cd /Users/hkarabas/IdeaProjects/untitled && mvn clean package -DskipTests

# Run Docker
docker-compose up -d

# Wait
sleep 15

# Access Swagger UI
open http://localhost:8080/swagger-ui/index.html

# Or curl
curl http://localhost:8080/api/books
```

That's it! Your API with Swagger documentation is now running! 🚀

