# Keycloak Integration Guide

## Overview

This guide covers the complete Keycloak OAuth2/OpenID Connect integration with the Books API using MySQL as the database backend.

## Architecture

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Client    │◄───────►│   Keycloak   │◄───────►│ MySQL (KC)  │
│ (Browser/   │  OAuth2  │   Server     │  JDBC   │  Database   │
│  Postman)   │         │  (Port 8180) │         └─────────────┘
└─────────────┘         └──────────────┘
       │                                          
       │ JWT Token                                
       │                                          
       ▼                                          
┌─────────────┐         ┌─────────────┐
│  Books API  │◄───────►│ MySQL (App) │
│ Spring Boot │  JDBC   │  Database   │
│ (Port 8080) │         └─────────────┘
└─────────────┘
```

## Components

### 1. Docker Services

The docker-compose.yml defines 4 services:

- **mysql**: Application database (books_db)
- **keycloak-mysql**: Keycloak database (keycloak)
- **keycloak**: Keycloak authentication server (port 8180)
- **app**: Spring Boot application (port 8080)

### 2. Keycloak Configuration

**Realm**: `books-realm`
- Isolated authentication/authorization domain
- Contains clients, users, and roles

**Client**: `books-service`
- OAuth2/OpenID Connect client
- Client ID: `books-service`
- Client Secret: `books-secret-2024`
- Confidential client (requires secret)
- Enabled flows: Authorization Code, Direct Access Grants

**Roles**:
- `USER`: Basic user access
- `ADMIN`: Administrative access

**Users**:
- Admin user (username: `admin`, password: `admin123`, role: `ADMIN`)
- Regular user (username: `user`, password: `user123`, role: `USER`)

### 3. Spring Security Configuration

**Security Filter Chain**:
- Public endpoints: `/swagger-ui/**`, `/v3/api-docs/**`, `/api/public/**`
- User endpoints: `/api/books/**` (requires `USER` or `ADMIN` role)
- Admin endpoints: `/api/admin/**` (requires `ADMIN` role)

**JWT Token Processing**:
- Validates tokens against Keycloak
- Extracts roles from `realm_access` and `resource_access` claims
- Converts roles to Spring Security authorities with `ROLE_` prefix

## Setup Instructions

### Step 1: Start Docker Services

```bash
cd /Users/hkarabas/IdeaProjects/untitled

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f keycloak

# Wait for Keycloak to be ready (look for "started in" message)
```

### Step 2: Configure Keycloak

Run the automated setup script:

```bash
./keycloak_setup.sh
```

This script will:
1. Wait for Keycloak to be ready
2. Create the `books-realm` realm
3. Create `USER` and `ADMIN` roles
4. Create the `books-service` client with secret
5. Create test users (`admin` and `user`)
6. Assign roles to users

**Manual Setup** (if you prefer GUI):

1. Access Keycloak Admin Console: http://localhost:8180
2. Login with admin/admin
3. Create realm: `books-realm`
4. Create client:
   - Client ID: `books-service`
   - Client Protocol: `openid-connect`
   - Access Type: `confidential`
   - Valid Redirect URIs: `http://localhost:8080/*`
   - Go to Credentials tab and copy the secret
5. Create roles: `USER`, `ADMIN`
6. Create users and assign roles

### Step 3: Update Application Configuration

Update `src/main/resources/application.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-secret: books-secret-2024  # Use your actual client secret
```

### Step 4: Rebuild and Restart Application

```bash
# Rebuild the application
mvn clean package -DskipTests

# Restart the app service
docker-compose restart app

# Or rebuild and restart
docker-compose up -d --build app
```

## API Endpoints

### Public Endpoints (No Authentication)

```bash
# Public information
GET /api/public/info

# Health check
GET /api/health
```

### User Endpoints (USER or ADMIN role)

```bash
# Get user profile
GET /api/user/profile
Authorization: Bearer {access_token}

# Get all JWT claims
GET /api/user/claims
Authorization: Bearer {access_token}

# Book operations
GET /api/books
GET /api/books/{id}
POST /api/books
PUT /api/books/{id}
DELETE /api/books/{id}
```

### Admin Endpoints (ADMIN role only)

```bash
# Admin dashboard
GET /api/admin/dashboard
Authorization: Bearer {access_token}

# List users
GET /api/admin/users
Authorization: Bearer {access_token}
```

## Testing

### Automated Testing

Run the comprehensive test script:

```bash
./test_keycloak.sh
```

This script tests:
- Public endpoint access (no auth)
- User authentication and authorization
- Admin authentication and authorization
- Role-based access control
- JWT token validation
- Unauthorized access attempts

### Manual Testing with cURL

#### 1. Get Access Token (User)

```bash
curl -X POST 'http://localhost:8180/realms/books-realm/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=books-service' \
  -d 'client_secret=books-secret-2024' \
  -d 'grant_type=password' \
  -d 'username=user' \
  -d 'password=user123'
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 3600,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer"
}
```

#### 2. Get Access Token (Admin)

```bash
curl -X POST 'http://localhost:8180/realms/books-realm/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=books-service' \
  -d 'client_secret=books-secret-2024' \
  -d 'grant_type=password' \
  -d 'username=admin' \
  -d 'password=admin123'
```

#### 3. Access Protected Endpoint

```bash
# Replace {access_token} with the actual token from step 1 or 2
curl -X GET 'http://localhost:8080/api/user/profile' \
  -H 'Authorization: Bearer {access_token}'
```

#### 4. Test Admin Endpoint (will fail with USER token)

```bash
curl -X GET 'http://localhost:8080/api/admin/dashboard' \
  -H 'Authorization: Bearer {user_access_token}'
```

Expected: 403 Forbidden

```bash
curl -X GET 'http://localhost:8080/api/admin/dashboard' \
  -H 'Authorization: Bearer {admin_access_token}'
```

Expected: 200 OK with dashboard data

### Testing with Postman

#### Setup

1. Create a new request collection
2. Add environment variables:
   - `keycloak_url`: http://localhost:8180
   - `api_url`: http://localhost:8080
   - `client_id`: books-service
   - `client_secret`: books-secret-2024
   - `username`: user or admin
   - `password`: user123 or admin123

#### Get Token Request

```
POST {{keycloak_url}}/realms/books-realm/protocol/openid-connect/token

Body (x-www-form-urlencoded):
  client_id: {{client_id}}
  client_secret: {{client_secret}}
  grant_type: password
  username: {{username}}
  password: {{password}}

Tests (JavaScript):
  var jsonData = pm.response.json();
  pm.environment.set("access_token", jsonData.access_token);
```

#### Protected Request

```
GET {{api_url}}/api/user/profile

Headers:
  Authorization: Bearer {{access_token}}
```

### Testing with Swagger UI

1. Open Swagger UI: http://localhost:8080/swagger-ui.html
2. Click "Authorize" button
3. Choose one of:
   - **Bearer JWT**: Paste a token obtained from cURL/Postman
   - **OAuth2**: Use the authorization code flow (browser popup)
4. Test endpoints directly from Swagger

## Token Structure

### JWT Token Claims

```json
{
  "exp": 1737590400,
  "iat": 1737586800,
  "jti": "e1b2c3d4-5678-90ab-cdef-1234567890ab",
  "iss": "http://localhost:8180/realms/books-realm",
  "sub": "f1e2d3c4-b5a6-7890-1234-567890abcdef",
  "typ": "Bearer",
  "azp": "books-service",
  "session_state": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "realm_access": {
    "roles": ["USER"]
  },
  "resource_access": {
    "books-service": {
      "roles": ["books-reader"]
    }
  },
  "scope": "openid profile email",
  "email_verified": true,
  "name": "Regular User",
  "preferred_username": "user",
  "given_name": "Regular",
  "family_name": "User",
  "email": "user@books.com"
}
```

### Role Extraction

The `SecurityConfig` extracts roles from:
1. **Realm roles**: `realm_access.roles[]`
2. **Client roles**: `resource_access.{client}.roles[]`
3. **Scopes**: Standard OAuth2 scopes

All roles are prefixed with `ROLE_` for Spring Security.

## Configuration Reference

### Application Properties

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Where to validate tokens
          issuer-uri: http://localhost:8180/realms/books-realm
          # Public keys for signature verification
          jwk-set-uri: http://localhost:8180/realms/books-realm/protocol/openid-connect/certs
      client:
        registration:
          keycloak:
            client-id: books-service
            client-secret: books-secret-2024
            scope: openid,profile,email
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
        provider:
          keycloak:
            issuer-uri: http://localhost:8180/realms/books-realm
            authorization-uri: http://localhost:8180/realms/books-realm/protocol/openid-connect/auth
            token-uri: http://localhost:8180/realms/books-realm/protocol/openid-connect/token
            user-info-uri: http://localhost:8180/realms/books-realm/protocol/openid-connect/userinfo
            jwk-set-uri: http://localhost:8180/realms/books-realm/protocol/openid-connect/certs
            user-name-attribute: preferred_username
```

### Docker Environment Variables

**For Application**:
```yaml
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/books-realm
SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_ISSUER_URI: http://keycloak:8080/realms/books-realm
```

**For Keycloak**:
```yaml
KC_DB: mysql
KC_DB_URL: jdbc:mysql://keycloak-mysql:3306/keycloak?useSSL=false&allowPublicKeyRetrieval=true
KC_DB_USERNAME: keycloak
KC_DB_PASSWORD: keycloak_pass
KEYCLOAK_ADMIN: admin
KEYCLOAK_ADMIN_PASSWORD: admin
KC_HOSTNAME: localhost
KC_HTTP_ENABLED: "true"
```

## Troubleshooting

### Issue: Cannot connect to Keycloak

**Solution**:
```bash
# Check if Keycloak is running
docker ps | grep keycloak

# Check Keycloak logs
docker logs keycloak

# Check Keycloak health
curl http://localhost:8180/health
```

### Issue: 401 Unauthorized

**Causes**:
- Token expired (default: 1 hour)
- Invalid token
- Token from wrong realm
- Keycloak server unreachable

**Solution**:
```bash
# Get a new token
curl -X POST 'http://localhost:8180/realms/books-realm/protocol/openid-connect/token' \
  -d 'client_id=books-service' \
  -d 'client_secret=books-secret-2024' \
  -d 'grant_type=password' \
  -d 'username=user' \
  -d 'password=user123'
```

### Issue: 403 Forbidden

**Cause**: User doesn't have required role

**Solution**: Verify user roles in Keycloak Admin Console or use the correct user.

### Issue: Client secret not found

**Solution**:
1. Login to Keycloak Admin Console
2. Navigate to: Realm → Clients → books-service → Credentials
3. Copy the secret
4. Update `application.yml`

### Issue: Database connection error

**Solution**:
```bash
# Check MySQL containers
docker ps | grep mysql

# Check Keycloak MySQL logs
docker logs keycloak-mysql

# Restart services
docker-compose restart
```

## Security Best Practices

1. **Production Configuration**:
   - Use HTTPS for all connections
   - Set `KC_HOSTNAME_STRICT: "true"`
   - Use strong client secrets (not the example secret)
   - Enable SSL for database connections

2. **Token Management**:
   - Store tokens securely (httpOnly cookies for web apps)
   - Implement token refresh logic
   - Set appropriate token lifespans

3. **Role-Based Access**:
   - Use fine-grained roles
   - Implement resource-level permissions
   - Regular audit of user roles

4. **Monitoring**:
   - Enable Keycloak metrics
   - Monitor failed authentication attempts
   - Log security events

## Additional Resources

- Keycloak Documentation: https://www.keycloak.org/documentation
- Spring Security OAuth2: https://spring.io/guides/tutorials/spring-boot-oauth2/
- OpenID Connect Specification: https://openid.net/connect/

## Quick Reference

### Access URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Keycloak Admin | http://localhost:8180 | admin / admin |
| Books API | http://localhost:8080 | N/A (uses tokens) |
| Swagger UI | http://localhost:8080/swagger-ui.html | N/A |
| OpenAPI Docs | http://localhost:8080/v3/api-docs | N/A |

### Test Users

| Username | Password | Roles |
|----------|----------|-------|
| admin | admin123 | ADMIN |
| user | user123 | USER |

### Scripts

| Script | Purpose |
|--------|---------|
| `keycloak_setup.sh` | Automated Keycloak configuration |
| `test_keycloak.sh` | Automated API testing |
| `docker-compose up -d` | Start all services |
| `docker-compose logs -f keycloak` | View Keycloak logs |
| `docker-compose down -v` | Stop and remove all data |

