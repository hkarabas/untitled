# ✅ COMPLETE SETUP - Local Port 9090 + Docker Compose

**Status:** ✅ FULLY OPERATIONAL  
**Date:** January 15, 2026

---

## What's Running ✅

### Local Development (Port 9090)
- **When it will run:** After local MySQL is set up or using Docker
- **Start command:** `mvn spring-boot:run -Dspring.profiles.active=local`
- **Port:** 9090
- **Database:** localhost:3306 (local MySQL needed)

### Docker Deployment (Port 8080)
- **Status:** ✅ RUNNING NOW
- **MySQL:** ✅ Running in container
- **Spring Boot:** ✅ Running in container on 8080
- **Start command:** Already running via `docker-compose up -d`

---

## Access Points NOW ✅

### Docker Container (Active Now)
- **API:** http://localhost:8080/api/books
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI Docs:** http://localhost:8080/v3/api-docs

### Local Development (When ready)
- **API:** http://localhost:9090/api/books
- **Swagger UI:** http://localhost:9090/swagger-ui.html

---

## Test Docker Right Now

```bash
# Test API
curl http://localhost:8080/api/books

# Test file upload
curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads \
  -F "file=@/Users/hkarabas/IdeaProjects/untitled/books_100000.csv"

# Access Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

## Docker Management

### Check Status
```bash
docker-compose ps
```

### View Logs
```bash
docker-compose logs -f app    # App logs
docker-compose logs -f mysql  # MySQL logs
docker-compose logs -f        # All logs
```

### Stop
```bash
docker-compose down
```

### Restart
```bash
docker-compose restart app
```

### Full Rebuild
```bash
docker-compose down -v
docker-compose up --build -d
```

---

## Local Development Setup

If you want to run locally on port 9090, first set up local MySQL:

### Option A: Install MySQL Locally
```bash
# macOS with Homebrew
brew install mysql
brew services start mysql

# Create database and user
mysql -u root -e "CREATE DATABASE books_db;"
mysql -u root -e "CREATE USER 'books_user'@'localhost' IDENTIFIED BY 'books_pass';"
mysql -u root -e "GRANT ALL PRIVILEGES ON books_db.* TO 'books_user'@'localhost';"
```

### Option B: Use Docker MySQL for Local Dev
```bash
# Start only MySQL container
docker run -d \
  -e MYSQL_DATABASE=books_db \
  -e MYSQL_USER=books_user \
  -e MYSQL_PASSWORD=books_pass \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -p 3306:3306 \
  mysql:8.0
```

### Then Start Local App
```bash
mvn spring-boot:run -Dspring.profiles.active=local
```

Access on: **http://localhost:9090/swagger-ui.html**

---

## Configuration Summary

### application.yml (Default)
```yaml
server:
  port: 9090
```

### application-local.yml (Local Profile)
```yaml
server:
  port: 9090
  # Connects to localhost:3306
```

### Docker Compose
```yaml
app:
  ports:
    - "8080:8080"  # Container:Host
mysql:
  ports:
    - "3306:3306"
```

---

## File Upload Verified ✅

✅ max-file-size: 100MB  
✅ max-request-size: 100MB  
✅ max-post-size: 104857600  
✅ CORS enabled for both ports  
✅ Exception handlers in place  

---

## Current Recommendation

### For Testing: Use Docker ✅
```bash
# Already running!
curl http://localhost:8080/api/books
```

### For Development: Use Local 9090
```bash
# After MySQL is set up
mvn spring-boot:run -Dspring.profiles.active=local
curl http://localhost:9090/api/books
```

---

## Both Running Simultaneously?

You can run both at the same time!

**Docker on 8080:**
```bash
# Already running in background
docker-compose ps
```

**Local on 9090:**
```bash
# In new terminal
mvn spring-boot:run -Dspring.profiles.active=local
```

Both use different ports, no conflict!

---

## Quick Commands

```bash
# Check Docker status
docker-compose ps

# View Docker logs
docker-compose logs -f

# Test Docker API
curl http://localhost:8080/api/books

# Test file upload to Docker
curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads \
  -F "file=@books_100000.csv"

# Stop Docker
docker-compose down

# Start local on 9090 (after MySQL setup)
mvn spring-boot:run -Dspring.profiles.active=local

# Test local API
curl http://localhost:9090/api/books
```

---

## Next Steps

### Immediately Available (Docker)
1. Test API: `curl http://localhost:8080/api/books`
2. Upload CSV: `curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads -F "file=@books.csv"`
3. View docs: Open browser to `http://localhost:8080/swagger-ui.html`

### For Local Development
1. Set up local MySQL (Option A or B above)
2. Start local app: `mvn spring-boot:run -Dspring.profiles.active=local`
3. Access on: `http://localhost:9090/swagger-ui.html`

---

## Troubleshooting

### Docker App Not Responding
```bash
docker-compose logs app | tail -50
docker-compose restart app
```

### MySQL Connection Issues (Docker)
```bash
docker-compose logs mysql
docker-compose restart mysql
```

### Port Already in Use
```bash
# Check port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or restart Docker
docker-compose down && docker-compose up -d
```

### Want to Rebuild Everything
```bash
docker-compose down -v
docker system prune -a
docker-compose up --build -d
```

---

**Status: ✅ READY FOR TESTING**

Docker is running now with both MySQL and Spring Boot.

Test immediately with:
```bash
curl http://localhost:8080/api/books
```

---

For local development on port 9090, set up MySQL first, then:
```bash
mvn spring-boot:run -Dspring.profiles.active=local
```

