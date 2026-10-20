# 🚀 Postman JWT Quick Start - 3 Steps

## Step 1️⃣: Import Collection

### Option A: Auto-Import (Easiest)
```bash
# Copy the collection file to Postman downloads
# Then in Postman: File → Import → Browse → Select collection
# Or drag-drop into Postman window
```

### Option B: Manual Setup
1. Open Postman
2. Create new **Environment** called `Books-API-Dev`
3. Add variables:
   ```
   keycloak_url = http://localhost:8180
   api_url = http://localhost:8080
   client_id = books-service
   client_secret = books-secret-2024
   username = user
   password = user123
   jwt_token = (empty)
   refresh_token = (empty)
   ```
4. Create new **Collection** called `Books-API`

---

## Step 2️⃣: Login Request

### Create Login Request

```
Name: LOGIN - Get JWT Token
Method: POST
URL: {{keycloak_url}}/realms/books-realm/protocol/openid-connect/token
```

**Headers Tab:**
```
Content-Type: application/x-www-form-urlencoded
```

**Body Tab** (select form-data):
```
grant_type       = password
client_id        = {{client_id}}
client_secret    = {{client_secret}}
username         = {{username}}
password         = {{password}}
scope            = openid profile email
```

**Tests Tab** (copy-paste):
```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("jwt_token", jsonData.access_token);
    pm.environment.set("refresh_token", jsonData.refresh_token);
    console.log("✅ JWT Token obtained!");
}
```

**Click Send** ✓

---

## Step 3️⃣: GetBooks Request

### Create GetBooks Request

```
Name: GET BOOKS - Get all books
Method: GET
URL: {{api_url}}/api/books
```

**Headers Tab:**
```
Authorization: Bearer {{jwt_token}}
Content-Type: application/json
```

**Click Send** ✓

**Expected Response:**
```json
[
  {
    "id": 1,
    "title": "Book Title",
    "author": "Author Name",
    "price": 12.99,
    "createdAt": "2024-01-25T10:00:00",
    "updatedAt": "2024-01-25T10:00:00"
  }
]
```

---

## ✅ Verification Checklist

- [ ] Keycloak running: `docker-compose ps` shows `keycloak` running
- [ ] Application running: `docker-compose ps` shows `app` running
- [ ] Login request returns 200 with `access_token`
- [ ] GetBooks request returns 200 with list of books
- [ ] JWT token starts with `eyJ`
- [ ] Token visible in Postman Environment variables

---

## 🔑 Test Credentials

### Regular User
```
Username: user
Password: user123
```

### Admin User
```
Username: admin
Password: admin123
```

---

## 🎯 Quick Links

- **Keycloak Admin**: http://localhost:8180/admin (admin/admin123)
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs

---

## 🚨 Troubleshooting

| Problem | Solution |
|---------|----------|
| 401 on GetBooks | Re-login, get new token |
| Connection refused | Run `docker-compose up -d` |
| Invalid credentials | Use correct username/password |
| Token expired | Re-login or use refresh token |
| CORS error | Check SecurityConfig.java |

---

## 📚 Collection File

The collection JSON file includes:
- ✅ 1. LOGIN - Get JWT Token
- ✅ 2. GET BOOKS - Get all books
- ✅ 3. GET BOOK BY ID
- ✅ 4. CREATE BOOK
- ✅ 5. DELETE BOOK
- ✅ 6. USER PROFILE
- ✅ 7. JWT CLAIMS
- ✅ 8. ADMIN DASHBOARD (Admin only)
- ✅ 9. ADMIN USERS (Admin only)
- ✅ 10. PUBLIC INFO (No auth)
- ✅ 11. REFRESH TOKEN

**File**: `Books_API_JWT_Collection.postman_collection.json`

---

## 🎬 Workflow Example

```
1. START HERE: LOGIN request
   ↓ (Gets jwt_token)
2. GET BOOKS request
   ↓ (Returns list of books)
3. GET BOOK BY ID request
   ↓ (Get specific book)
4. CREATE BOOK request
   ↓ (Add new book)
5. DELETE BOOK request
   ↓ (Remove book)
```

---

## 💡 Pro Tips

1. **Auto-token refresh**: Add Pre-request Script to check token expiry
2. **Save responses**: Click Save → Save as Example
3. **Run collection**: Use Collection Runner to test all requests
4. **Monitor requests**: Open Console (Cmd+Alt+C on Mac)
5. **Share collection**: Export and share with team

---

## 📊 Response Examples

### Login Success (200)
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "expires_in": 300,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
  "token_type": "Bearer",
  "scope": "openid profile email"
}
```

### GetBooks Success (200)
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
]
```

### Unauthorized (401)
```json
{
  "error": "invalid_token",
  "error_description": "Token is invalid"
}
```

---

**Ready to test?** Start with Step 1 above! 🚀

