# Keycloak OAuth2/OpenID Connect Integration - README

## 🔐 What's Been Implemented

Complete Keycloak integration with Spring Boot Books API using MySQL backend for both application and Keycloak databases.

### Architecture Components

1. **Docker Services** (4 containers):
   - `mysql`: Application database (books_db)
   - `keycloak-mysql`: Keycloak database (keycloak)  
   - `keycloak`: Authentication/Authorization server (port 8180)
   - `app`: Spring Boot application (port 8080)

2. **Security Layer**:
   - OAuth2 Resource Server (JWT validation)
   - OAuth2 Client (Authorization Code flow)
   - Role-based access control (USER, ADMIN)
   - Public endpoints (no auth)

3. **Keycloak Configuration**:
   - Realm: `books-realm`
   - Client: `books-service`
   - Roles: `USER`, `ADMIN`
   - Test users: `admin`, `user`

## 📁 Files Created/Modified

### New Files

1. **Configuration**
   - `src/main/java/com/example/untitled/config/SecurityConfig.java` - Spring Security + OAuth2 configuration
   
2. **Controllers**
   - `src/main/java/com/example/untitled/controller/AuthController.java` - Secured endpoints demo

3. **Scripts**
   - `keycloak_setup.sh` - Automated Keycloak configuration
   - `test_keycloak.sh` - Automated API testing

4. **Documentation**
   - `KEYCLOAK_INTEGRATION_GUIDE.md` - Complete integration guide
   - `KEYCLOAK_QUICK_START.md` - 5-minute quick start
   - `README_KEYCLOAK.md` - This file

### Modified Files

1. `pom.xml` - Added Spring Security & OAuth2 dependencies
2. `docker-compose.yml` - Added Keycloak and keycloak-mysql services
3. `src/main/resources/application.yml` - Added OAuth2 configuration
4. `src/main/java/com/example/untitled/config/OpenApiConfig.java` - Added security schemes to Swagger

## 🚀 Quick Start (5 Minutes)

### Step 1: Start Docker Services

```bash
cd /Users/hkarabas/IdeaProjects/untitled
docker-compose up -d
```

Wait for Keycloak to start (~30-60 seconds). Check status:
```bash
docker-compose logs -f keycloak | grep "started in"
```

### Step 2: Configure Keycloak

```bash
./keycloak_setup.sh
```

This creates:
- ✓ Realm: books-realm
- ✓ Client: books-service (secret: books-secret-2024)
- ✓ Roles: USER, ADMIN
- ✓ Users: admin (admin123), user (user123)

### Step 3: Build & Deploy

```bash
# Build
mvn clean package -DskipTests

# Restart application
docker-compose restart app

# Or rebuild container
docker-compose up -d --build app
```

### Step 4: Test

```bash
./test_keycloak.sh
```

## 🧪 Manual Testing

### Get Access Token

**For Admin User:**
```bash
curl -X POST 'http://localhost:8180/realms/books-realm/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=books-service' \
  -d 'client_secret=books-secret-2024' \
  -d 'grant_type=password' \
  -d 'username=admin' \
  -d 'password=admin123' | jq -r '.access_token'
```

**For Regular User:**
```bash
curl -X POST 'http://localhost:8180/realms/books-realm/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=books-service' \
  -d 'client_secret=books-secret-2024' \
  -d 'grant_type=password' \
  -d 'username=user' \
  -d 'password=user123' | jq -r '.access_token'
```

### Test Endpoints

**Public Endpoint (No Auth Required):**
```bash
curl http://localhost:8080/api/public/info | jq .
```

**User Endpoint (USER or ADMIN role):**
```bash
TOKEN="your-access-token-here"

curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/user/profile | jq .
```

**Admin Endpoint (ADMIN role only):**
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/dashboard | jq .
```

## 📋 API Endpoints

### Authentication Endpoints

| Method | Endpoint | Roles | Description |
|--------|----------|-------|-------------|
| GET | `/api/public/info` | Public | Public information |
| GET | `/api/health` | Public | Health check |
| GET | `/api/user/profile` | USER, ADMIN | Get user profile from JWT |
| GET | `/api/user/claims` | USER, ADMIN | Get all JWT claims |
| GET | `/api/admin/dashboard` | ADMIN | Admin dashboard |
| GET | `/api/admin/users` | ADMIN | List users (mock) |

### Book Endpoints (Existing)

All book endpoints now require authentication:
- `/api/books/**` - Requires USER or ADMIN role

## 🔧 Configuration

### Application Configuration

`src/main/resources/application.yml`:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/books-realm
      client:
        registration:
          keycloak:
            client-id: books-service
            client-secret: books-secret-2024
```

### Docker Configuration

`docker-compose.yml`:
```yaml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:23.0
    ports:
      - "8180:8080"
    environment:
      KC_DB: mysql
      KC_DB_URL: jdbc:mysql://keycloak-mysql:3306/keycloak
```

## 🔑 Test Credentials

### Keycloak Admin Console
- URL: http://localhost:8180
- Username: `admin`
- Password: `admin`

### Test Users

| Username | Password | Role | Use Case |
|----------|----------|------|----------|
| admin | admin123 | ADMIN | Full access to all endpoints |
| user | user123 | USER | Limited access (no admin endpoints) |

## 🎯 Access URLs

| Service | URL | Purpose |
|---------|-----|---------|
| Keycloak Admin | http://localhost:8180 | Manage realms, users, roles |
| Books API | http://localhost:8080/api | REST API endpoints |
| Swagger UI | http://localhost:8080/swagger-ui.html | API documentation & testing |
| OpenAPI Docs | http://localhost:8080/v3/api-docs | OpenAPI specification |

## 🧩 Security Configuration Details

### SecurityConfig.java

**Key Features:**
- JWT token validation against Keycloak
- Role extraction from `realm_access` and `resource_access` claims
- Public endpoints whitelist
- Role-based authorization (@PreAuthorize)
- Stateless session management

**Role Mapping:**
- Keycloak roles → Spring Security authorities
- Prefix: `ROLE_` (e.g., USER → ROLE_USER)
- Sources: Realm roles + Client roles

### Token Processing

1. **Validation**: Token signature verified using Keycloak's public keys
2. **Expiration**: Default 1 hour (configurable in Keycloak)
3. **Claims**: Username, email, roles, scopes extracted
4. **Authorities**: Roles converted to GrantedAuthority objects

## 📚 Testing Scenarios

### Scenario 1: Public Access
```bash
# Anyone can access public endpoints
curl http://localhost:8080/api/public/info
# Expected: 200 OK
```

### Scenario 2: User Access
```bash
# Get token for user
TOKEN=$(curl -s -X POST 'http://localhost:8180/realms/books-realm/protocol/openid-connect/token' \
  -d 'client_id=books-service' -d 'client_secret=books-secret-2024' \
  -d 'grant_type=password' -d 'username=user' -d 'password=user123' | jq -r '.access_token')

# Access user endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/user/profile
# Expected: 200 OK with profile data
```

### Scenario 3: Unauthorized Access
```bash
# Try admin endpoint with user token
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin/dashboard
# Expected: 403 Forbidden
```

### Scenario 4: Admin Access
```bash
# Get token for admin
ADMIN_TOKEN=$(curl -s -X POST 'http://localhost:8180/realms/books-realm/protocol/openid-connect/token' \
  -d 'client_id=books-service' -d 'client_secret=books-secret-2024' \
  -d 'grant_type=password' -d 'username=admin' -d 'password=admin123' | jq -r '.access_token')

# Access admin endpoint
curl -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8080/api/admin/dashboard
# Expected: 200 OK with dashboard data
```

### Scenario 5: No Token
```bash
# Try protected endpoint without token
curl http://localhost:8080/api/user/profile
# Expected: 401 Unauthorized
```

## 🐛 Troubleshooting

### Issue: Dependencies not resolved
```bash
cd /Users/hkarabas/IdeaProjects/untitled
mvn clean install
# Or
mvn dependency:resolve
```

### Issue: Keycloak not starting
```bash
# Check logs
docker logs keycloak

# Restart
docker-compose restart keycloak

# Full restart
docker-compose down
docker-compose up -d
```

### Issue: 401 Unauthorized
**Cause**: Token expired (default 1 hour)
**Solution**: Get a new token

### Issue: 403 Forbidden
**Cause**: User doesn't have required role
**Solution**: Use admin user for admin endpoints

### Issue: Can't get token
```bash
# Verify Keycloak is ready
curl http://localhost:8180/health/ready

# Re-run setup
./keycloak_setup.sh
```

### Issue: Client secret mismatch
1. Login to Keycloak: http://localhost:8180
2. Navigate to: Realm → Clients → books-service → Credentials
3. Copy the secret
4. Update `application.yml`

## 📖 Documentation

- **Quick Start**: [KEYCLOAK_QUICK_START.md](KEYCLOAK_QUICK_START.md) - Get up and running in 5 minutes
- **Integration Guide**: [KEYCLOAK_INTEGRATION_GUIDE.md](KEYCLOAK_INTEGRATION_GUIDE.md) - Detailed documentation
- **Scripts**:
  - `keycloak_setup.sh` - Automated Keycloak configuration
  - `test_keycloak.sh` - Automated testing

## 🔐 Security Best Practices

### Development
- ✓ HTTP is acceptable for localhost
- ✓ Client secret in config files
- ✓ Test users with simple passwords

### Production
- ⚠️ Use HTTPS for all connections
- ⚠️ Store client secrets in environment variables
- ⚠️ Use strong, unique passwords
- ⚠️ Enable strict hostname verification
- ⚠️ Set appropriate token lifespans
- ⚠️ Use SSL for database connections
- ⚠️ Regular security audits

## 🎓 Learning Resources

- **Keycloak**: https://www.keycloak.org/documentation
- **Spring Security OAuth2**: https://spring.io/guides/tutorials/spring-boot-oauth2/
- **JWT**: https://jwt.io/introduction
- **OpenID Connect**: https://openid.net/connect/

## ✅ Verification Checklist

After setup, verify:

- [ ] All Docker containers running: `docker ps`
- [ ] Keycloak accessible: http://localhost:8180
- [ ] Keycloak admin login works (admin/admin)
- [ ] Realm `books-realm` exists
- [ ] Client `books-service` configured
- [ ] Roles `USER` and `ADMIN` created
- [ ] Test users `admin` and `user` exist
- [ ] Application running: http://localhost:8080
- [ ] Swagger UI accessible: http://localhost:8080/swagger-ui.html
- [ ] Can get access token for admin user
- [ ] Can get access token for regular user
- [ ] Public endpoints accessible without token
- [ ] Protected endpoints require valid token
- [ ] Admin endpoints reject USER role

## 🚀 Next Steps

1. **Secure Existing Endpoints**: Add `@PreAuthorize` to BookController methods
2. **Custom Roles**: Create fine-grained roles (e.g., BOOK_READER, BOOK_WRITER)
3. **Token Refresh**: Implement refresh token logic
4. **Frontend Integration**: Add login UI with redirect flow
5. **Monitoring**: Enable Keycloak metrics and event logging
6. **Production Config**: Use environment-specific configuration

## 📞 Support

For issues or questions:
1. Check [KEYCLOAK_INTEGRATION_GUIDE.md](KEYCLOAK_INTEGRATION_GUIDE.md)
2. Review Keycloak logs: `docker logs keycloak`
3. Review application logs: `docker logs app`
4. Test with automated script: `./test_keycloak.sh`

---

**Implementation Complete** ✅

All Keycloak integration code has been implemented in agent mode with MySQL backend support.

