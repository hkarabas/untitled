# 🔐 Postman JWT Login & GetBooks Guide

## Overview

This guide shows you how to:
1. **Login to Keycloak** and get a JWT token
2. **Use JWT token** to authenticate API requests
3. **Call getBooks endpoint** with JWT authorization

---

## ✅ Prerequisites

- ✅ Docker running (`docker-compose up -d`)
- ✅ Keycloak configured (`./keycloak_setup.sh`)
- ✅ Application started on port 8080
- ✅ Postman installed ([Download](https://www.postman.com/downloads/))

---

## 🔑 Step 1: Get JWT Token (Login Request)

### Request Details

**Method**: `POST`  
**URL**: `http://localhost:8180/realms/books-realm/protocol/openid-connect/token`

### Headers

```
Content-Type: application/x-www-form-urlencoded
```

### Body (Form Data)

```
grant_type=password
client_id=books-service
client_secret=books-secret-2024
username=user
password=user123
scope=openid profile email
```

### In Postman

1. Create new **POST** request
2. URL: `http://localhost:8180/realms/books-realm/protocol/openid-connect/token`
3. Go to **Body** tab
4. Select **form-data** (NOT raw)
5. Add these key-value pairs:
   - `grant_type` = `password`
   - `client_id` = `books-service`
   - `client_secret` = `books-secret-2024`
   - `username` = `user`
   - `password` = `user123`
   - `scope` = `openid profile email`

6. Click **Send**

### Expected Response

```json
{
    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cC...",
    "expires_in": 300,
    "refresh_expires_in": 1800,
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cC...",
    "token_type": "Bearer",
    "not-before-policy": 0,
    "session_state": "abcd1234...",
    "scope": "openid profile email"
}
```

### 💾 Save Token to Postman Variable

1. Click on the response
2. Go to **Tests** tab of the login request
3. Add this script:

```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("jwt_token", jsonData.access_token);
    pm.environment.set("token_expires_in", jsonData.expires_in);
    console.log("JWT Token saved: " + jsonData.access_token.substring(0, 50) + "...");
}
```

4. Click **Send** again - token will auto-save
5. Verify in **Environment** section (top right, gear icon)

---

## 📚 Step 2: Call GetBooks with JWT Token

### Request Details

**Method**: `GET`  
**URL**: `http://localhost:8080/api/books`

### Headers (Manual Setup)

```
Authorization: Bearer <YOUR_JWT_TOKEN>
Content-Type: application/json
```

### In Postman (Easy Method with Variable)

1. Create new **GET** request
2. URL: `http://localhost:8080/api/books`
3. Go to **Headers** tab
4. Add header:
   - **Key**: `Authorization`
   - **Value**: `Bearer {{jwt_token}}`  ← Use double braces!

5. Click **Send**

### Expected Response (200 OK)

```json
[
    {
        "id": 1,
        "title": "The Great Gatsby",
        "author": "F. Scott Fitzgerald",
        "price": 12.99,
        "createdAt": "2024-01-25T10:00:00",
        "updatedAt": "2024-01-25T10:00:00"
    },
    {
        "id": 2,
        "title": "To Kill a Mockingbird",
        "author": "Harper Lee",
        "price": 14.99,
        "createdAt": "2024-01-25T10:05:00",
        "updatedAt": "2024-01-25T10:05:00"
    }
    // ... more books
]
```

---

## 🔄 Step 3: Complete Postman Workflow

### Method 1: Manual Token Copy (Quick Test)

1. **Login Request** - Get token
2. Copy the `access_token` value
3. Go to **GetBooks** request
4. In Headers, paste: `Bearer <token>`
5. Send

### Method 2: Automatic with Variables (Recommended)

#### Setup Environment

1. Click **Environments** (left sidebar)
2. Click **Create** new environment
3. Name it: `Books-API-Dev`
4. Add variables:
   - **keycloak_url** = `http://localhost:8180`
   - **api_url** = `http://localhost:8080`
   - **client_id** = `books-service`
   - **client_secret** = `books-secret-2024`
   - **username** = `user`
   - **password** = `user123`
   - **jwt_token** = (empty - will be filled by login)

5. Click **Save**

#### Create Collection

1. Click **Collections** (left sidebar)
2. Click **Create** new collection
3. Name it: `Books-API`

#### Add Requests to Collection

**Request 1: Login**
```
POST {{keycloak_url}}/realms/books-realm/protocol/openid-connect/token

Body (form-data):
grant_type: password
client_id: {{client_id}}
client_secret: {{client_secret}}
username: {{username}}
password: {{password}}
scope: openid profile email

Tests Tab:
if (pm.response.code === 200) {
    pm.environment.set("jwt_token", pm.response.json().access_token);
    console.log("✅ Token obtained successfully");
}
```

**Request 2: Get Books**
```
GET {{api_url}}/api/books

Headers:
Authorization: Bearer {{jwt_token}}

Tests Tab:
pm.test("Status code is 200", function() {
    pm.response.to.have.status(200);
});

pm.test("Response is array", function() {
    pm.expect(pm.response.json()).to.be.an('array');
});

pm.test("Contains book objects", function() {
    var books = pm.response.json();
    if (books.length > 0) {
        pm.expect(books[0]).to.have.property('id');
        pm.expect(books[0]).to.have.property('title');
        pm.expect(books[0]).to.have.property('author');
    }
});
```

---

## 🧪 Test Different User Roles

### Admin User

Login with admin credentials to test admin endpoints:

```
grant_type: password
client_id: books-service
client_secret: books-secret-2024
username: admin
password: admin123
scope: openid profile email
```

Then access:
- `GET {{api_url}}/api/admin/dashboard`
- `GET {{api_url}}/api/admin/users`

### Regular User

```
grant_type: password
client_id: books-service
client_secret: books-secret-2024
username: user
password: user123
scope: openid profile email
```

Can access:
- `GET {{api_url}}/api/books`
- `GET {{api_url}}/api/user/profile`
- `GET {{api_url}}/api/user/claims`

---

## 🔍 Decode JWT Token

Visit [jwt.io](https://jwt.io) to inspect your token:

1. Copy the `access_token` from login response
2. Paste into jwt.io decoder
3. See payload structure:

```json
{
  "exp": 1707291234,
  "iat": 1707291234,
  "jti": "abc123...",
  "iss": "http://localhost:8180/realms/books-realm",
  "aud": "account",
  "sub": "user123...",
  "typ": "Bearer",
  "azp": "books-service",
  "session_state": "xyz789...",
  "name": "Test User",
  "given_name": "Test",
  "family_name": "User",
  "preferred_username": "user",
  "email": "user@example.com",
  "realm_access": {
    "roles": ["USER", "default-roles-books-realm"]
  }
}
```

---

## ⏰ Token Expiration & Refresh

### Token Lifetime

- **Access Token** expires in: `300 seconds` (5 minutes)
- **Refresh Token** expires in: `1800 seconds` (30 minutes)

### Refresh Token Request

When access token expires, use refresh token:

```
POST http://localhost:8180/realms/books-realm/protocol/openid-connect/token

Body (form-data):
grant_type: refresh_token
client_id: books-service
client_secret: books-secret-2024
refresh_token: {{refresh_token}}
```

Add to Tests:
```javascript
if (pm.response.code === 200) {
    pm.environment.set("jwt_token", pm.response.json().access_token);
    pm.environment.set("refresh_token", pm.response.json().refresh_token);
}
```

---

## 🚨 Common Issues & Solutions

### Issue 1: 401 Unauthorized on GetBooks

**Symptom**: Response shows `401 Unauthorized`

**Solution**:
- ✅ Check token is in Bearer format: `Bearer eyJ...`
- ✅ Verify token hasn't expired (login again)
- ✅ Ensure Keycloak is running: `docker-compose ps`
- ✅ Check Authorization header is spelled correctly

### Issue 2: Keycloak Connection Error

**Symptom**: `Connection refused` to port 8180

**Solution**:
```bash
# Check if containers are running
docker-compose ps

# Restart Keycloak
docker-compose restart keycloak

# View logs
docker-compose logs keycloak
```

### Issue 3: Invalid Client Credentials

**Symptom**: `invalid_client` error

**Solution**:
- ✅ Verify credentials match setup:
  - client_id: `books-service`
  - client_secret: `books-secret-2024`
- ✅ Run setup script: `./keycloak_setup.sh`

### Issue 4: User Not Found

**Symptom**: `invalid_grant` error

**Solution**:
- ✅ Check username exists in Keycloak:
  - Admin: `admin` / `admin123`
  - User: `user` / `user123`
- ✅ Verify in Keycloak Admin Console:
  - http://localhost:8180/admin
  - Users → Users

---

## 📊 All Available Endpoints

### Public (No Auth)

```
GET  http://localhost:8080/api/public/info
GET  http://localhost:8080/actuator/health
```

### User Endpoints (USER or ADMIN role required)

```
GET  http://localhost:8080/api/books                    # Get all books
GET  http://localhost:8080/api/books/{id}               # Get book by ID
POST http://localhost:8080/api/books                    # Create book
DELETE http://localhost:8080/api/books/{id}             # Delete book

GET  http://localhost:8080/api/user/profile             # Get user profile
GET  http://localhost:8080/api/user/claims              # Get JWT claims
```

### Admin Endpoints (ADMIN role only)

```
GET  http://localhost:8080/api/admin/dashboard          # Admin dashboard
GET  http://localhost:8080/api/admin/users              # List users
```

---

## 🎯 Quick Reference

| Action | URL | Method | Auth |
|--------|-----|--------|------|
| **Login** | `{{keycloak_url}}/realms/books-realm/protocol/openid-connect/token` | POST | ✗ |
| **Get Books** | `{{api_url}}/api/books` | GET | ✓ Bearer Token |
| **Get One Book** | `{{api_url}}/api/books/1` | GET | ✓ Bearer Token |
| **Create Book** | `{{api_url}}/api/books` | POST | ✓ Bearer Token |
| **Delete Book** | `{{api_url}}/api/books/1` | DELETE | ✓ Bearer Token |
| **User Profile** | `{{api_url}}/api/user/profile` | GET | ✓ Bearer Token |
| **Admin Dashboard** | `{{api_url}}/api/admin/dashboard` | GET | ✓ Bearer Token (Admin) |

---

## 📝 Postman Script Examples

### Auto-Login and GetBooks

Create a folder with requests in order:

1. **Pre-request Script** (collection level):
```javascript
// Auto-login if token is missing or about to expire
if (!pm.environment.has("jwt_token") || 
    Date.now() > pm.environment.get("token_expires_at")) {
    
    console.log("🔐 Getting new token...");
    // Login request will set the token
}
```

2. **Test Script** (login request):
```javascript
if (pm.response.code === 200) {
    let token = pm.response.json().access_token;
    let expiresIn = pm.response.json().expires_in;
    
    pm.environment.set("jwt_token", token);
    pm.environment.set("token_expires_at", Date.now() + (expiresIn * 1000));
    
    console.log("✅ Token obtained. Valid for " + expiresIn + " seconds");
}
```

3. **GetBooks request**:
```
GET {{api_url}}/api/books

Headers:
Authorization: Bearer {{jwt_token}}
```

---

## 🎬 Video Steps (Textual)

### Step-by-step Walkthrough

**Part 1: Login**
```
1. New Request → POST
2. URL: http://localhost:8180/realms/books-realm/protocol/openid-connect/token
3. Headers: Content-Type: application/x-www-form-urlencoded
4. Body → form-data:
   - grant_type = password
   - client_id = books-service
   - client_secret = books-secret-2024
   - username = user
   - password = user123
   - scope = openid profile email
5. Send ✓
6. Copy access_token from response
```

**Part 2: GetBooks**
```
1. New Request → GET
2. URL: http://localhost:8080/api/books
3. Headers → Authorization: Bearer <paste_token>
4. Send ✓
5. See list of books in response
```

---

## ✨ Pro Tips

1. **Save Requests**: Use Collections to organize your requests
2. **Use Environments**: Never hardcode values, use variables
3. **Auto-login**: Set up pre-request scripts to auto-refresh tokens
4. **Test Scripts**: Add assertions to validate responses
5. **Monitor**: Use Console (Cmd+Alt+C) to see all requests/responses
6. **Export**: Share your collection with team via Export

---

## 📚 Related Documentation

- [KEYCLOAK_QUICK_START.md](KEYCLOAK_QUICK_START.md) - Keycloak setup
- [KEYCLOAK_INTEGRATION_GUIDE.md](KEYCLOAK_INTEGRATION_GUIDE.md) - Integration details
- [API_USAGE_GUIDE.md](API_USAGE_GUIDE.md) - Full API reference

---

**Created**: 2026-01-25  
**Status**: ✅ Ready to use

