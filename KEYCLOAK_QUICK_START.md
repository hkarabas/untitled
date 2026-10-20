# Keycloak Quick Start Guide

Get Keycloak authentication running in 5 minutes!

## Prerequisites

- Docker and Docker Compose installed
- Maven installed
- jq installed (for scripts): `brew install jq` (macOS)

## Quick Start

### 1. Start Services (2 minutes)

```bash
cd /Users/hkarabas/IdeaProjects/untitled

# Start all services (MySQL, Keycloak, App)
docker-compose up -d

# Wait for Keycloak (about 30-60 seconds)
echo "Waiting for Keycloak..."
until curl -sf http://localhost:8180/health/ready > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo " Ready!"
```

### 2. Configure Keycloak (1 minute)

```bash
# Run the automated setup script
./keycloak_setup.sh
```

This creates:
- Realm: `books-realm`
- Client: `books-service` (secret: `books-secret-2024`)
- Users: `admin` (password: admin123) and `user` (password: user123)
- Roles: `ADMIN` and `USER`

### 3. Update Application Config (30 seconds)

The application.yml already has the correct client secret. If you changed it during setup, update it:

```yaml
# src/main/resources/application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-secret: books-secret-2024  # Use your actual secret
```

### 4. Rebuild and Restart App (1 minute)

```bash
# Rebuild
mvn clean package -DskipTests

# Restart app
docker-compose restart app

# Or rebuild and restart
docker-compose up -d --build app
```

### 5. Test It! (30 seconds)

```bash
# Run automated tests
./test_keycloak.sh
```

Or manually:

```bash
# Get token
TOKEN=$(curl -s -X POST 'http://localhost:8180/realms/books-realm/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=books-service' \
  -d 'client_secret=books-secret-2024' \
  -d 'grant_type=password' \
  -d 'username=admin' \
  -d 'password=admin123' | jq -r '.access_token')

# Test protected endpoint
curl -X GET 'http://localhost:8080/api/user/profile' \
  -H "Authorization: Bearer $TOKEN" | jq .
```

## Test Endpoints

### Public (No Auth)
```bash
curl http://localhost:8080/api/public/info | jq .
```

### User (USER or ADMIN role)
```bash
# Get user profile
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/user/profile | jq .

# Get JWT claims
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/user/claims | jq .
```

### Admin (ADMIN role only)
```bash
# Admin dashboard
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/dashboard | jq .

# List users
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/users | jq .
```

## Access Points

- **Keycloak Admin**: http://localhost:8180 (admin/admin)
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs

## Test Credentials

| User | Password | Role |
|------|----------|------|
| admin | admin123 | ADMIN |
| user | user123 | USER |

## Common Commands

```bash
# View Keycloak logs
docker-compose logs -f keycloak

# View app logs
docker-compose logs -f app

# Restart everything
docker-compose restart

# Stop everything
docker-compose down

# Stop and remove all data
docker-compose down -v
```

## Using Swagger UI

1. Open http://localhost:8080/swagger-ui.html
2. Click "Authorize" button
3. Select "bearer-jwt"
4. Get a token:
   ```bash
   curl -X POST 'http://localhost:8180/realms/books-realm/protocol/openid-connect/token' \
     -d 'client_id=books-service' \
     -d 'client_secret=books-secret-2024' \
     -d 'grant_type=password' \
     -d 'username=admin' \
     -d 'password=admin123' | jq -r '.access_token'
   ```
5. Paste the token
6. Click "Authorize"
7. Try endpoints!

## Troubleshooting

### Keycloak not starting
```bash
# Check logs
docker logs keycloak

# Restart
docker-compose restart keycloak
```

### Can't get token
```bash
# Verify Keycloak is ready
curl http://localhost:8180/health

# Re-run setup script
./keycloak_setup.sh
```

### 401 Unauthorized
```bash
# Token might be expired (1 hour lifetime)
# Get a fresh token using the curl command above
```

### 403 Forbidden
```bash
# User doesn't have required role
# Use admin user for admin endpoints
# Use user or admin for user endpoints
```

## What's Next?

See [KEYCLOAK_INTEGRATION_GUIDE.md](KEYCLOAK_INTEGRATION_GUIDE.md) for:
- Detailed architecture
- Production configuration
- Security best practices
- Advanced testing scenarios
- Token management
- Custom role mapping

---

**Total setup time: ~5 minutes** ⚡

