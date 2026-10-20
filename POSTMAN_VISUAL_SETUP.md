# 📸 Postman JWT Setup - Visual Step-by-Step

## 🎯 Goal
Login via Keycloak and use JWT to call `GET /api/books` endpoint

---

## 📋 Prerequisites

- ✅ Docker running
- ✅ Services started: `docker-compose up -d`
- ✅ Keycloak configured: `./keycloak_setup.sh`
- ✅ Postman installed
- ✅ Application on port 8080

---

## 🔴 PART 1: CREATE ENVIRONMENT IN POSTMAN

### Step 1: Open Environments

```
Postman Window
├── Left Sidebar: "Environments"
└── Click on "Environments"
```

### Step 2: Create New Environment

```
Click [+] or Create Button
↓
Name: Books-API-Dev
↓
Add Variables (click Add Row):
```

| Variable Name | Initial Value | Current Value |
|---|---|---|
| `keycloak_url` | `http://localhost:8180` | `http://localhost:8180` |
| `api_url` | `http://localhost:8080` | `http://localhost:8080` |
| `client_id` | `books-service` | `books-service` |
| `client_secret` | `books-secret-2024` | `books-secret-2024` |
| `username` | `user` | `user` |
| `password` | `user123` | `user123` |
| `jwt_token` | `` | `` |
| `refresh_token` | `` | `` |

### Step 3: Save Environment

```
Click [Save] Button
✅ Environment created!
```

### Step 4: Select Environment

```
Top-right of Postman Window
Dropdown showing "No Environment" 
↓
Select "Books-API-Dev"
✅ Environment is now ACTIVE (shown in orange)
```

---

## 🔵 PART 2: CREATE LOGIN REQUEST

### Step 1: Create New Request

```
Top-left: [+] New Tab
↓
Select "HTTP Request"
↓
Name it: "1. LOGIN - Get JWT Token"
```

### Step 2: Set Method & URL

```
METHOD: POST (dropdown)
URL: {{keycloak_url}}/realms/books-realm/protocol/openid-connect/token
```

### Step 3: Add Headers

```
Headers Tab (click it)
```

| Key | Value |
|---|---|
| `Content-Type` | `application/x-www-form-urlencoded` |

### Step 4: Add Body Parameters

```
Body Tab
↓
Select "form-data" (NOT raw)
↓
Add rows:
```

| Key | Value | Type |
|---|---|---|
| `grant_type` | `password` | Text |
| `client_id` | `{{client_id}}` | Text |
| `client_secret` | `{{client_secret}}` | Text |
| `username` | `{{username}}` | Text |
| `password` | `{{password}}` | Text |
| `scope` | `openid profile email` | Text |

### Step 5: Add Test Script

```
Tests Tab
↓
Paste this code:
```

```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("jwt_token", jsonData.access_token);
    pm.environment.set("refresh_token", jsonData.refresh_token);
    
    console.log("✅ JWT Token obtained successfully!");
    console.log("Token expires in: " + jsonData.expires_in + " seconds");
    console.log("Token: " + jsonData.access_token.substring(0, 50) + "...");
}

pm.test("Status code is 200", function() {
    pm.response.to.have.status(200);
});

pm.test("Response contains access_token", function() {
    pm.expect(pm.response.json()).to.have.property('access_token');
});
```

### Step 6: Send Request

```
[Send] Button (top-right)
↓
Response should be (200 OK):
```

```json
{
    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 300,
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "scope": "openid profile email"
}
```

### Step 7: Verify Token Saved

```
Top-right: Environment dropdown
↓
Click gear icon "Books-API-Dev"
↓
Check jwt_token variable is populated
✅ Should show token starting with "eyJ"
```

---

## 🟢 PART 3: CREATE GETBOOKS REQUEST

### Step 1: Create New Request

```
Top-left: [+] New Tab
↓
Name it: "2. GET BOOKS - Get all books"
```

### Step 2: Set Method & URL

```
METHOD: GET (dropdown)
URL: {{api_url}}/api/books
```

### Step 3: Add Headers

```
Headers Tab
```

| Key | Value |
|---|---|
| `Authorization` | `Bearer {{jwt_token}}` |
| `Content-Type` | `application/json` |

### Step 4: Add Test Script

```
Tests Tab
↓
Paste:
```

```javascript
pm.test("Status code is 200", function() {
    pm.response.to.have.status(200);
});

pm.test("Response is an array", function() {
    pm.expect(pm.response.json()).to.be.an('array');
});

if (pm.response.json().length > 0) {
    pm.test("First book has required fields", function() {
        var book = pm.response.json()[0];
        pm.expect(book).to.have.property('id');
        pm.expect(book).to.have.property('title');
        pm.expect(book).to.have.property('author');
    });
}

console.log("✅ Retrieved " + pm.response.json().length + " books");
```

### Step 5: Send Request

```
[Send] Button
↓
Response should be (200 OK):
```

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

### Step 6: Verify in Console

```
View → Show Postman Console (Cmd+Alt+C on Mac)
↓
Should see:
✅ Retrieved X books
```

---

## 🎬 COMPLETE WORKFLOW

```
┌─────────────────────────────────────┐
│ 1. SELECT ENVIRONMENT               │
│    "Books-API-Dev" (top-right)       │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│ 2. SEND LOGIN REQUEST               │
│    POST /token                      │
│    ↓                                 │
│    jwt_token saved to environment   │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│ 3. SEND GETBOOKS REQUEST            │
│    GET /api/books                   │
│    Headers: Authorization: Bearer.. │
│    ↓                                 │
│    ✅ Returns list of books          │
└─────────────────────────────────────┘
```

---

## 🔑 Test Different Users

### Test with Regular User

```
Environment Variables:
username: user
password: user123
↓
Send LOGIN request
↓
Send GETBOOKS request
↓
✅ Works! Can read books
```

### Test with Admin User

```
Environment Variables:
username: admin
password: admin123
↓
Send LOGIN request
↓
Try ADMIN endpoint:
GET {{api_url}}/api/admin/dashboard
↓
✅ Admin access granted
```

---

## 🚨 Troubleshooting

### Problem: 401 Unauthorized on GetBooks

**Symptoms:**
```
Response: 401 Unauthorized
Body: {error: "invalid_token"}
```

**Solutions:**
1. ✅ Re-send LOGIN request to get fresh token
2. ✅ Verify `Bearer` is in Authorization header (with space)
3. ✅ Check token isn't expired (expires in 5 minutes)
4. ✅ Verify environment is selected

### Problem: Connection Refused on Login

**Symptoms:**
```
Error: connect ECONNREFUSED 127.0.0.1:8180
```

**Solutions:**
```bash
# Check containers
docker-compose ps

# Should show:
# keycloak    Up
# mysql       Up
# app         Up

# If not running:
docker-compose up -d

# Check Keycloak logs
docker-compose logs keycloak
```

### Problem: Invalid Credentials

**Symptoms:**
```
Response: 400 Bad Request
Body: {error: "invalid_grant"}
```

**Solutions:**
1. ✅ Verify username/password in environment:
   - User: `user` / `user123`
   - Admin: `admin` / `admin123`
2. ✅ Run setup script: `./keycloak_setup.sh`
3. ✅ Check in Keycloak Admin:
   - http://localhost:8180/admin
   - Login as admin/admin123
   - Users section → verify user exists

---

## 📊 Check Request/Response Details

### View Full Request

```
Postman Window
↓
Bottom left: "Request" tab
↓
See all headers, body, parameters
```

### View Full Response

```
Response body
↓
Click "Pretty" (formatted JSON)
↓
Scroll through all fields
```

### View Console Logs

```
Menu → View → Show Postman Console
Or: Cmd+Alt+C (Mac) / Ctrl+Alt+C (Windows)
↓
See all console.log() outputs
```

---

## 🎯 Success Indicators

✅ **LOGIN Request**
- Status: 200 OK
- Has: access_token, expires_in, token_type: "Bearer"
- jwt_token variable populated
- Console: "✅ JWT Token obtained successfully!"

✅ **GETBOOKS Request**
- Status: 200 OK
- Response: Array of book objects
- Each book has: id, title, author, price
- Console: "✅ Retrieved X books"

---

## 💾 Save Your Work

### Save as Postman Collection

```
Collections (left sidebar)
↓
Right-click "Books-API"
↓
Export
↓
Save as: "Books_API_JWT_Collection.json"
```

### Share with Team

```
Export collection → Share file
↓
Team member imports:
   File → Import → select JSON
↓
They get all requests with variables
```

---

## 🔐 Security Notes

- ⚠️ Never commit credentials to Git
- ⚠️ Use environment variables for sensitive data
- ⚠️ Don't share collection with real passwords
- ⚠️ Token expires in 300 seconds - re-login if needed
- ⚠️ Use HTTPS in production (not http)

---

## 🎓 Learning Path

1. ✅ **Step 1**: Create Environment
2. ✅ **Step 2**: Create Login Request
3. ✅ **Step 3**: Create GetBooks Request
4. 📚 **Next**: Try other endpoints (Create, Delete, Admin)
5. 📚 **Advanced**: Setup Pre-request Scripts for auto-login

---

## 📝 Quick Reference

### Login Endpoint
```
Method: POST
URL: {{keycloak_url}}/realms/books-realm/protocol/openid-connect/token
Grant Type: password
```

### GetBooks Endpoint
```
Method: GET
URL: {{api_url}}/api/books
Header: Authorization: Bearer {{jwt_token}}
```

### Credentials
```
User: user / user123
Admin: admin / admin123
```

### Token Details
```
Expires: 300 seconds (5 minutes)
Type: Bearer JWT
Scope: openid profile email
```

---

**You're all set! Follow the steps above and you'll be calling GetBooks with JWT authentication in Postman! 🚀**

