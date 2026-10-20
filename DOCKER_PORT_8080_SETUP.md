# Docker Setup - Port 8080

**Status:** ✅ Configured for port 8080  
**Date:** January 15, 2026

---

## Port Configuration

### ✅ Updated Ports
- application.yml: Changed from 8181 → **8080**
- application-local.yml: Changed from 8181 → **8080**
- Docker Dockerfile: Exposes **8080** (already configured)
- Docker Compose: Maps **8080:8080** (already configured)

---

## Quick Start with Docker

### Prerequisites
- Docker installed
- Docker Compose installed
- MySQL will run in container

### Build and Run

```bash
# Navigate to project
cd /Users/hkarabas/IdeaProjects/untitled

# Build Docker image
docker build -t books-api:latest .

# Run with Docker Compose (includes MySQL)
docker-compose up -d

# Check logs
docker-compose logs -f app
```

### Access API
```
http://localhost:8080/swagger-ui.html
http://localhost:8080/api/books
```

---

## Docker Compose Services

### MySQL Database
- **Image:** mysql:8.0
- **Port:** 3306
- **Database:** books_db
- **User:** books_user
- **Password:** books_pass
- **Auto-healthcheck:** Yes

### Spring Boot App
- **Build:** From Dockerfile
- **Port:** 8080
- **Depends On:** MySQL (waits for healthcheck)
- **Debug Port:** 5005 (optional)

---

## Detailed Docker Commands

### Build Only
```bash
docker build -t books-api:latest .
```

### Run with Docker Compose
```bash
# Start in background
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Run Docker Image Directly
```bash
# Start MySQL first
docker run -d \
  -e MYSQL_DATABASE=books_db \
  -e MYSQL_USER=books_user \
  -e MYSQL_PASSWORD=books_pass \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -p 3306:3306 \
  mysql:8.0

# Run app
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/books_db \
  books-api:latest
```

---

## File Upload Test

### Using curl
```bash
curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads \
  -F "file=@/path/to/books.csv"
```

### Expected Response (Success)
```
HTTP/1.1 200 OK
"CSV import completed successfully (Virtual Threads method) in XXXX ms"
```

### If Still Getting 413
The configuration includes:
- `max-file-size: 100MB`
- `max-request-size: 100MB`
- `max-post-size: 104857600`

This should handle large files. If you still get 413:
1. Check file size: `ls -lh your_file.csv`
2. File must be < 100MB
3. Restart containers: `docker-compose restart app`

---

## Environment Variables

### In Docker Compose
- `SPRING_DATASOURCE_URL` - Database connection
- `SPRING_DATASOURCE_USERNAME` - Database user
- `SPRING_DATASOURCE_PASSWORD` - Database password
- `SPRING_PROFILES_ACTIVE` - Active profile (optional)

### Set Active Profile
```yaml
environment:
  SPRING_PROFILES_ACTIVE: local
```

Or via command line:
```bash
docker-compose up -e SPRING_PROFILES_ACTIVE=local
```

---

## Troubleshooting

### Container Won't Start
```bash
# Check logs
docker-compose logs app

# Restart
docker-compose restart app

# Full rebuild
docker-compose down
docker-compose up --build -d
```

### Database Connection Issues
```bash
# Check MySQL is running
docker-compose ps

# Test MySQL connection
docker-compose exec mysql mysql -u books_user -p -e "SELECT VERSION();"
```

### Port Already in Use
```bash
# Check what's using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use different port in docker-compose.yml
ports:
  - "8000:8080"  # Container 8080 → Host 8000
```

### Clear Everything and Start Fresh
```bash
docker-compose down -v
docker system prune -a
docker-compose up --build -d
```

---

## Monitor Docker

### Check Status
```bash
docker-compose ps
```

### View Logs
```bash
# App logs
docker-compose logs app

# MySQL logs
docker-compose logs mysql

# Follow in real-time
docker-compose logs -f
```

### Execute Commands
```bash
# Into app container
docker-compose exec app bash

# Into MySQL
docker-compose exec mysql bash

# Run curl inside app container
docker-compose exec app curl http://localhost:8080/api/books
```

---

## Performance

### Configure Resources (docker-compose.yml)
```yaml
app:
  deploy:
    resources:
      limits:
        cpus: '1'
        memory: 2G
      reservations:
        cpus: '0.5'
        memory: 1G
```

### Connection Pool
Already configured in application.yml:
- Max: 30 connections
- Min Idle: 15 connections
- Good for Docker environment

---

## Production Considerations

### Use Environment Variables
```yaml
environment:
  SPRING_DATASOURCE_URL: ${DB_URL}
  SPRING_DATASOURCE_USERNAME: ${DB_USER}
  SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
```

### Disable Debug Port
```dockerfile
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
# Remove: -agentlib:jdwp=...
```

### Use Secrets
```yaml
secrets:
  db_password:
    file: ./secrets/db_password

environment:
  SPRING_DATASOURCE_PASSWORD_FILE: /run/secrets/db_password
```

---

## Files Used

| File | Purpose |
|------|---------|
| Dockerfile | Build image |
| docker-compose.yml | Orchestrate containers |
| application.yml | Port 8080 config |
| application-local.yml | Port 8080 config |

---

## Summary

✅ **Port changed to 8080** in all configs  
✅ **Docker Dockerfile ready** for port 8080  
✅ **Docker Compose ready** with MySQL  
✅ **File upload configured** for 100MB max  
✅ **CORS enabled** for frontend access  

---

## Next Steps

1. **Build and Run:**
   ```bash
   docker-compose up --build -d
   ```

2. **Test API:**
   ```bash
   curl http://localhost:8080/api/books
   ```

3. **Upload CSV:**
   ```bash
   curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads \
     -F "file=@books.csv"
   ```

4. **Access Swagger UI:**
   ```
   http://localhost:8080/swagger-ui.html
   ```

---

**Status: ✅ READY FOR DOCKER**

Everything configured for port 8080 and Docker deployment!

