# Application Configuration Profiles

**Date:** January 15, 2026  
**Version:** 1.0.0  
**Status:** ✅ Ready to Use

---

## Overview

The application now includes environment-specific configuration profiles:

1. **application.yml** - Default (base configuration)
2. **application-local.yml** - Local development (port 8181)
3. **application-staging.yml** - Staging environment (port 8080)
4. **application-prod.yml** - Production (port 8080, SSL, security)

---

## Profiles Explained

### 1. application-local.yml
**For:** Local development on your machine  
**Port:** 8181  
**Database:** `localhost:3306`  
**Features:**
- Debug logging enabled
- Swagger UI enabled
- File uploads: 100MB max
- Connection pool: 30 connections
- Thread pool: 200 threads

**When to use:** `mvn spring-boot:run -Dspring.profiles.active=local`

---

### 2. application-staging.yml
**For:** Staging/testing environment  
**Port:** 8080  
**Database:** Configurable via environment variable  
**Features:**
- Info level logging
- Swagger UI enabled
- File uploads: 100MB max
- Connection pool: 20 connections
- Thread pool: 100 threads

**When to use:** Testing before production

---

### 3. application-prod.yml
**For:** Production deployment  
**Port:** 8080  
**Database:** Requires environment variables  
**Features:**
- Error level logging only
- Swagger UI disabled
- File uploads: 100MB max
- Connection pool: 50 connections
- Thread pool: 300 threads
- SSL/TLS enabled
- Log rotation configured

**When to use:** Live production environment

---

## How to Run

### Option 1: Local Development (Recommended for Development)
```bash
mvn spring-boot:run -Dspring.profiles.active=local
```

Then access: `http://localhost:8181/swagger-ui.html`

### Option 2: Default Configuration
```bash
mvn spring-boot:run
```

Port: 8181 (configured in application.yml)

### Option 3: Staging
```bash
mvn spring-boot:run -Dspring.profiles.active=staging
```

### Option 4: Production
```bash
mvn spring-boot:run -Dspring.profiles.active=prod
```

### Option 5: Using JAR with Profile
```bash
java -jar target/untitled-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### Option 6: Using Environment Variable
```bash
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run
```

---

## Configuration Comparison

| Setting | Local | Staging | Prod |
|---------|-------|---------|------|
| **Port** | 8181 | 8080 | 8080 |
| **Log Level** | DEBUG | INFO | ERROR |
| **Swagger UI** | ✅ Enabled | ✅ Enabled | ❌ Disabled |
| **Max Pool Size** | 30 | 20 | 50 |
| **Max Threads** | 200 | 100 | 300 |
| **SSL/TLS** | ❌ No | ❌ No | ✅ Yes |
| **File Upload** | 100MB | 100MB | 100MB |
| **Log File** | Console | Console | File + Rotation |
| **DB SSL** | No | Yes | Yes |

---

## Database Connection

### Local (application-local.yml)
```yaml
url: jdbc:mysql://localhost:3306/books_db
username: books_user
password: books_pass
```

**Setup:**
```bash
# Create database
mysql -u root -p
CREATE DATABASE books_db;
CREATE USER 'books_user'@'localhost' IDENTIFIED BY 'books_pass';
GRANT ALL PRIVILEGES ON books_db.* TO 'books_user'@'localhost';
FLUSH PRIVILEGES;
```

### Staging (application-staging.yml)
```yaml
url: jdbc:mysql://staging-db-server:3306/books_db
username: ${DB_USER}
password: ${DB_PASSWORD}
```

**Run with:**
```bash
export DB_USER=staging_user
export DB_PASSWORD=staging_password
mvn spring-boot:run -Dspring.profiles.active=staging
```

### Production (application-prod.yml)
```yaml
url: jdbc:mysql://${DB_HOST}:3306/books_db
username: ${DB_USER}
password: ${DB_PASSWORD}
```

**Run with:**
```bash
export DB_HOST=prod-db-server
export DB_USER=prod_user
export DB_PASSWORD=prod_password
export SSL_KEYSTORE_PATH=/path/to/keystore.p12
export SSL_KEYSTORE_PASSWORD=keystore_password
java -jar target/untitled-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## File Locations

All configuration files are in:
```
src/main/resources/
├── application.yml              (Default/Base config)
├── application-local.yml        (Local development)
├── application-staging.yml      (Staging environment)
└── application-prod.yml         (Production)
```

---

## Quick Start

### Step 1: Clone/Setup
```bash
cd /Users/hkarabas/IdeaProjects/untitled
```

### Step 2: Build
```bash
mvn clean package -DskipTests
```

### Step 3: Run Locally
```bash
mvn spring-boot:run -Dspring.profiles.active=local
```

### Step 4: Access API
```
http://localhost:8181/swagger-ui.html
```

---

## Environment Variables

### For Local Development
No environment variables needed - uses defaults

### For Staging
```bash
export DB_USER=your_staging_user
export DB_PASSWORD=your_staging_password
```

### For Production
```bash
export DB_HOST=production-database-host
export DB_USER=production_user
export DB_PASSWORD=production_password
export SSL_KEYSTORE_PATH=/etc/ssl/keystore.p12
export SSL_KEYSTORE_PASSWORD=your_keystore_password
```

---

## Logging Configuration

### Local (DEBUG)
```
Shows all logs including debug information
Useful for development and troubleshooting
```

### Staging (INFO)
```
Shows important information
Warning and error logs
Useful for testing
```

### Production (ERROR)
```
Only error messages logged
File rotation every 10MB
Keeps 30 days of logs
Optimized for performance
```

---

## Features by Profile

### Local Profile Features
✅ Full debug logging  
✅ Interactive Swagger UI  
✅ Development-friendly defaults  
✅ Port 8181 (less common, avoids conflicts)  
✅ Console output only  

### Staging Profile Features
✅ Balanced logging (INFO level)  
✅ Swagger UI for testing  
✅ SSL-enabled database connection  
✅ Environment variable support  
✅ Console output  

### Production Profile Features
✅ Minimal logging (ERROR only)  
✅ Swagger UI disabled (security)  
✅ SSL/TLS enabled  
✅ File-based logging with rotation  
✅ High connection pool (50)  
✅ High thread pool (300)  
✅ SSL keystore configuration  

---

## Docker Support

### Build Docker Image
```bash
docker build -t books-api:latest .
```

### Run with Local Profile
```bash
docker run -p 8181:8181 \
  -e SPRING_PROFILES_ACTIVE=local \
  books-api:latest
```

### Run with Production Profile
```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=mysql-server \
  -e DB_USER=prod_user \
  -e DB_PASSWORD=secure_password \
  books-api:latest
```

---

## Troubleshooting

### Issue: Wrong Profile Used
**Solution:** Explicitly set profile
```bash
mvn spring-boot:run -Dspring.profiles.active=local
```

### Issue: Database Connection Failed
**Check:** 
- Database is running
- Credentials are correct
- Network connection available

### Issue: Swagger UI Not Loading
**Local/Staging:** Should work  
**Production:** Intentionally disabled (set `enabled: false`)  

To enable in production:
```yaml
springdoc:
  swagger-ui:
    enabled: true
```

### Issue: Port Already in Use
**For Local:** Change port in application-local.yml
```yaml
server:
  port: 8182  # Or another available port
```

---

## Migration Between Environments

### From Local to Staging
1. Update database credentials
2. Set environment variables:
   ```bash
   export DB_USER=staging_user
   export DB_PASSWORD=staging_password
   ```
3. Run:
   ```bash
   mvn spring-boot:run -Dspring.profiles.active=staging
   ```

### From Staging to Production
1. Update all production credentials
2. Set all required environment variables
3. Build JAR:
   ```bash
   mvn clean package -DskipTests
   ```
4. Run with production profile:
   ```bash
   java -jar target/untitled-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
   ```

---

## Monitoring & Logs

### Local Development
Logs print to console in real-time

### Staging
Logs print to console during testing

### Production
Logs written to:
```
/var/log/books-api/application.log
```

View logs:
```bash
tail -f /var/log/books-api/application.log
```

---

## Best Practices

✅ **Local:** Use `local` profile for development  
✅ **Environment Variables:** Never hardcode production secrets  
✅ **Security:** Always enable SSL in production  
✅ **Logging:** Use appropriate log levels per environment  
✅ **Testing:** Test staging before production deployment  
✅ **Monitoring:** Monitor production logs and performance  

---

## Summary

| Profile | Use Case | Port | Swagger | SSL |
|---------|----------|------|---------|-----|
| local | Development | 8181 | ✅ | ❌ |
| staging | Pre-production testing | 8080 | ✅ | ✅ |
| prod | Live production | 8080 | ❌ | ✅ |

---

## Next Steps

1. **For Local Development:**
   ```bash
   mvn spring-boot:run -Dspring.profiles.active=local
   ```

2. **Access API:**
   ```
   http://localhost:8181/swagger-ui.html
   ```

3. **Start Building!** 🚀

---

**Status: ✅ Configuration Profiles Ready to Use**

All environment configurations are set up and ready for development, staging, and production deployment.

