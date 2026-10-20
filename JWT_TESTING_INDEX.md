# 🔐 JWT Authentication Testing Guide - INDEX

## 📚 Complete Documentation for JWT Login & GetBooks

This index provides quick access to all JWT authentication testing resources.

---

## 🎯 Quick Start (Choose Your Tool)

### 1️⃣ **Postman (Recommended for GUI Users)**

**Best for**: Visual interface, easy to use, great for teams

| Guide | Purpose | Best For |
|-------|---------|----------|
| [POSTMAN_QUICK_START.md](POSTMAN_QUICK_START.md) | 3-step quick setup | Fast setup in 5 minutes |
| [POSTMAN_VISUAL_SETUP.md](POSTMAN_VISUAL_SETUP.md) | Detailed visual walkthrough | Step-by-step screenshots |
| [POSTMAN_JWT_LOGIN_GETBOOKS.md](POSTMAN_JWT_LOGIN_GETBOOKS.md) | Complete reference guide | Full documentation |

**Collection File** (Ready to import):
- [Books_API_JWT_Collection.postman_collection.json](Books_API_JWT_Collection.postman_collection.json)
  - 11 pre-configured requests
  - All variables setup
  - Test scripts included
  - Just import and use!

---

### 2️⃣ **Curl Commands (Recommended for CLI Users)**

**Best for**: Terminal, scripting, automation

| Guide | Purpose | Best For |
|-------|---------|----------|
| [CURL_JWT_COMMANDS.md](CURL_JWT_COMMANDS.md) | All curl commands with examples | CLI testing & scripting |

**Script File** (Copy & run):
```bash
#!/bin/bash
# test_jwt.sh - Complete automated test script
# Gets token, calls API, displays results
```

---

## 🚀 Step-by-Step Comparison

### If you want **Postman (GUI)**:

```
1. Download Postman (free)
   ↓
2. Read: POSTMAN_QUICK_START.md (5 minutes)
   ↓
3. Import: Books_API_JWT_Collection.postman_collection.json
   ↓
4. Send LOGIN request → send GETBOOKS request
   ↓
✅ Done! See books in response
```

### If you want **Curl (CLI)**:

```
1. Open terminal
   ↓
2. Copy script from CURL_JWT_COMMANDS.md
   ↓
3. Run curl login command → get token
   ↓
4. Run curl getbooks command with token
   ↓
✅ Done! See JSON response
```

---

## 📊 File Overview

### Documentation Files (4 total)

| File | Type | Size | Content |
|------|------|------|---------|
| **POSTMAN_QUICK_START.md** | Guide | 📄 Short | 3-step setup, quick reference |
| **POSTMAN_VISUAL_SETUP.md** | Guide | 📘 Long | Visual walkthrough with steps |
| **POSTMAN_JWT_LOGIN_GETBOOKS.md** | Guide | 📖 Complete | Full reference with examples |
| **CURL_JWT_COMMANDS.md** | Guide | 📜 Complete | All curl commands + script |

### Collection/Script Files (2 total)

| File | Type | Usage |
|------|------|-------|
| **Books_API_JWT_Collection.postman_collection.json** | JSON | Import into Postman |
| **test_jwt.sh** | Bash | Run in terminal |

---

## 🎯 Choose Your Path

### Path 1: Postman (Visual - Recommended for Beginners)

**Time**: 5-10 minutes

```
START HERE
    ↓
[POSTMAN_QUICK_START.md]
    ↓
Confused? See detailed steps:
[POSTMAN_VISUAL_SETUP.md]
    ↓
Need more info? Full reference:
[POSTMAN_JWT_LOGIN_GETBOOKS.md]
    ↓
Import pre-made collection:
[Books_API_JWT_Collection.postman_collection.json]
    ↓
✅ LOGIN → GETBOOKS
```

### Path 2: Curl (CLI - Recommended for Developers)

**Time**: 5 minutes

```
START HERE
    ↓
[CURL_JWT_COMMANDS.md]
    ↓
Copy login command
    ↓
Copy getbooks command with token
    ↓
Run in terminal
    ↓
✅ See JSON response
```

---

## 🔑 Key Information Summary

### Keycloak Server
- **URL**: http://localhost:8180
- **Admin Portal**: http://localhost:8180/admin
- **Token Endpoint**: `/realms/books-realm/protocol/openid-connect/token`

### Application API
- **Base URL**: http://localhost:8080
- **Books Endpoint**: `/api/books`
- **Swagger UI**: http://localhost:8080/swagger-ui.html

### Credentials

| Role | Username | Password |
|------|----------|----------|
| Regular User | `user` | `user123` |
| Administrator | `admin` | `admin123` |

### OAuth2 Client
- **Client ID**: `books-service`
- **Client Secret**: `books-secret-2024`
- **Grant Type**: `password`

### Token Details
- **Type**: JWT (Bearer)
- **Expires In**: 300 seconds (5 minutes)
- **Scope**: `openid profile email`

---

## 📋 Working Checklist

### Before Testing
- [ ] Docker running: `docker-compose ps`
- [ ] Keycloak running (port 8180)
- [ ] MySQL running (port 3306)
- [ ] Application running (port 8080)
- [ ] Keycloak configured: `./keycloak_setup.sh` (already done)

### Testing with Postman
- [ ] Postman installed and opened
- [ ] Import collection JSON file
- [ ] Environment selected: "Books-API-Dev"
- [ ] Send LOGIN request ✓
- [ ] Check jwt_token in environment variables
- [ ] Send GETBOOKS request ✓
- [ ] See list of books in response

### Testing with Curl
- [ ] Terminal open
- [ ] jq installed (for JSON parsing)
- [ ] Copy login command
- [ ] Execute login command
- [ ] Save token to variable
- [ ] Copy getbooks command
- [ ] Execute with token
- [ ] See JSON response with books

---

## 🎯 Workflow Diagrams

### Postman Workflow

```
┌─────────────────────────┐
│ 1. Open Postman         │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│ 2. Import Collection    │
│    (.json file)         │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│ 3. Select Environment   │
│    (Books-API-Dev)      │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│ 4. Send LOGIN Request   │
│    (Get JWT token)      │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│ 5. Send GETBOOKS        │
│    (Use JWT token)      │
└────────────┬────────────┘
             ↓
┌─────────────────────────┐
│ ✅ See Books in Response│
└─────────────────────────┘
```

### Curl Workflow

```
┌──────────────────────────┐
│ 1. Copy login curl cmd   │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ 2. Run command           │
│    Get JWT token         │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ 3. Save token to variable│
│    TOKEN=$token          │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ 4. Copy getbooks curl cmd│
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ 5. Run with Bearer token │
│    -H "Bearer $TOKEN"    │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ ✅ See Books JSON        │
└──────────────────────────┘
```

---

## 🔍 Endpoint Reference

### Login (Get JWT Token)

```
POST http://localhost:8180/realms/books-realm/protocol/openid-connect/token

Body (form-data):
  grant_type: password
  client_id: books-service
  client_secret: books-secret-2024
  username: user
  password: user123
  scope: openid profile email

Response: 200 OK
  {
    "access_token": "eyJ...",
    "token_type": "Bearer",
    "expires_in": 300,
    "refresh_token": "eyJ..."
  }
```

### Get Books (Authenticated)

```
GET http://localhost:8080/api/books

Headers:
  Authorization: Bearer <jwt_token>
  Content-Type: application/json

Response: 200 OK
  [
    {
      "id": 1,
      "title": "...",
      "author": "...",
      "price": 12.99,
      "createdAt": "...",
      "updatedAt": "..."
    },
    ...
  ]
```

---

## 🚨 Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| **401 Unauthorized** on GetBooks | Expired/invalid token | Re-login, get fresh token |
| **Connection Refused** 8180 | Keycloak not running | `docker-compose up -d` |
| **Connection Refused** 8080 | App not running | Check container status |
| **Invalid Credentials** | Wrong username/password | Use correct: user/user123 |
| **jq not found** (curl) | jq not installed | `brew install jq` |
| **Bearer token not recognized** | Wrong format in header | Use: `Bearer <token>` (with space) |

---

## 🎓 Learning Resources

### Related Documentation
- [KEYCLOAK_QUICK_START.md](KEYCLOAK_QUICK_START.md) - Keycloak setup
- [KEYCLOAK_INTEGRATION_GUIDE.md](KEYCLOAK_INTEGRATION_GUIDE.md) - Integration details
- [API_USAGE_GUIDE.md](API_USAGE_GUIDE.md) - API reference
- [KEYCLOAK_COMPLETE.md](KEYCLOAK_COMPLETE.md) - Complete implementation

### External Resources
- [Postman Documentation](https://learning.postman.com/)
- [JWT.io](https://jwt.io) - Decode tokens
- [Keycloak Docs](https://www.keycloak.org/documentation)
- [Spring Security Docs](https://spring.io/projects/spring-security)

---

## 📞 Support

### Check Logs

```bash
# Keycloak logs
docker-compose logs keycloak

# Application logs
docker-compose logs app

# MySQL logs
docker-compose logs mysql
```

### Verify Setup

```bash
# Check containers running
docker-compose ps

# Restart services
docker-compose restart

# View configuration
cat application.yml
cat docker-compose.yml
```

### Test Health

```bash
# Public endpoint (no auth)
curl http://localhost:8080/api/public/info

# Swagger UI
open http://localhost:8080/swagger-ui.html

# Keycloak Admin
open http://localhost:8180/admin
```

---

## ✨ Pro Tips

1. **Save Token**: In Postman, use Tests tab script to auto-save token to environment
2. **Auto-refresh**: Create pre-request script to refresh expired tokens
3. **Share Collection**: Export collection for team collaboration
4. **Monitor**: Use Postman Console (Cmd+Alt+C) to see all requests
5. **Script**: Create bash script for automated testing in CI/CD
6. **Validate**: Use jwt.io to inspect token claims

---

## 📊 Document Quick Links

### For Postman Users
1. [Start here: POSTMAN_QUICK_START.md](POSTMAN_QUICK_START.md) - 5 min
2. [Visual guide: POSTMAN_VISUAL_SETUP.md](POSTMAN_VISUAL_SETUP.md) - Detailed
3. [Full reference: POSTMAN_JWT_LOGIN_GETBOOKS.md](POSTMAN_JWT_LOGIN_GETBOOKS.md) - Complete
4. [Collection file: Books_API_JWT_Collection.postman_collection.json](Books_API_JWT_Collection.postman_collection.json) - Import

### For Curl Users
1. [Start here: CURL_JWT_COMMANDS.md](CURL_JWT_COMMANDS.md) - All commands

---

## 🎬 Video-like Text Walkthrough

### Quick Start (2 minutes)

```
1. Open Postman ✓
2. Create environment with variables ✓
3. Create POST request to login endpoint ✓
4. Add body with credentials ✓
5. Send → get token ✓
6. Create GET request to /api/books ✓
7. Add Authorization header with token ✓
8. Send → see books ✓
DONE! ✅
```

---

## ✅ Success Criteria

You know it's working when:

- ✅ LOGIN request returns 200 with access_token
- ✅ Token starts with "eyJ" (JWT format)
- ✅ GETBOOKS request returns 200 with array of books
- ✅ Each book has id, title, author, price fields
- ✅ No 401 Unauthorized errors
- ✅ Console shows "✅ Retrieved X books"

---

## 🚀 Next Steps

After testing works:

1. **Create more requests**: Try other endpoints (Create, Delete, etc.)
2. **Test different roles**: Use admin account to access /api/admin
3. **Automate**: Create test scripts for CI/CD
4. **Monitor**: Setup health checks and alerts
5. **Document**: Add to your team's Postman workspace

---

## 📝 Summary

| Method | Tool | Setup Time | Ease | Best For |
|--------|------|------------|------|----------|
| **Postman** | GUI | 5-10 min | ⭐⭐⭐⭐⭐ | Teams, visual |
| **Curl** | CLI | 2-5 min | ⭐⭐⭐ | Developers, scripting |

**Recommendation**: Start with Postman for ease, then learn Curl for automation.

---

**Last Updated**: 2026-01-25  
**Status**: ✅ Complete & Ready to Use

