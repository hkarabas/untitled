# 🔗 Curl Commands - JWT Login & GetBooks

## Overview

If you prefer using `curl` commands instead of Postman, use these commands in your terminal.

---

## 🔑 Step 1: Login & Get JWT Token

### Command

```bash
curl -X POST \
  'http://localhost:8180/realms/books-realm/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=books-service' \
  -d 'client_secret=books-secret-2024' \
  -d 'username=user' \
  -d 'password=user123' \
  -d 'scope=openid profile email'
```

### Response (200 OK)

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "scope": "openid profile email"
}
```

### Save Token to Variable

```bash
# Get token and save to TOKEN variable
TOKEN=$(curl -s -X POST \
  'http://localhost:8180/realms/books-realm/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=books-service' \
  -d 'client_secret=books-secret-2024' \
  -d 'username=user' \
  -d 'password=user123' \
  -d 'scope=openid profile email' | jq -r '.access_token')

# Display token
echo "Token: $TOKEN"
```

---

## 📚 Step 2: Get Books with JWT Token

### Command (using saved token)

```bash
curl -X GET \
  'http://localhost:8080/api/books' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json'
```

### Response (200 OK)

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

---

## 🎯 Complete Script (All-in-One)

### Save as `test_jwt.sh`

```bash
#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
KEYCLOAK_URL="http://localhost:8180"
API_URL="http://localhost:8080"
CLIENT_ID="books-service"
CLIENT_SECRET="books-secret-2024"
USERNAME="user"
PASSWORD="user123"

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}JWT Login & GetBooks Test${NC}"
echo -e "${BLUE}======================================${NC}\n"

# Step 1: Login
echo -e "${BLUE}Step 1: Getting JWT Token...${NC}"
echo "Username: $USERNAME"
echo "Client: $CLIENT_ID\n"

RESPONSE=$(curl -s -X POST \
  "${KEYCLOAK_URL}/realms/books-realm/protocol/openid-connect/token" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d "grant_type=password" \
  -d "client_id=${CLIENT_ID}" \
  -d "client_secret=${CLIENT_SECRET}" \
  -d "username=${USERNAME}" \
  -d "password=${PASSWORD}" \
  -d "scope=openid profile email")

# Extract token
TOKEN=$(echo "$RESPONSE" | jq -r '.access_token')
EXPIRES=$(echo "$RESPONSE" | jq -r '.expires_in')

if [ "$TOKEN" = "null" ] || [ -z "$TOKEN" ]; then
    echo -e "${RED}❌ Login failed!${NC}"
    echo "Response: $RESPONSE"
    exit 1
fi

echo -e "${GREEN}✅ Login successful!${NC}"
echo "Token expires in: $EXPIRES seconds"
echo "Token: ${TOKEN:0:50}...\n"

# Step 2: Get Books
echo -e "${BLUE}Step 2: Getting Books...${NC}"

BOOKS_RESPONSE=$(curl -s -X GET \
  "${API_URL}/api/books" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json')

# Count books
BOOK_COUNT=$(echo "$BOOKS_RESPONSE" | jq 'length')

echo -e "${GREEN}✅ GetBooks successful!${NC}"
echo "Total books: $BOOK_COUNT\n"

# Display first 3 books
echo -e "${BLUE}First 3 Books:${NC}"
echo "$BOOKS_RESPONSE" | jq '.[0:3] | .[] | {id, title, author, price}'

echo -e "\n${BLUE}======================================${NC}"
echo -e "${GREEN}✅ JWT Test Complete!${NC}"
echo -e "${BLUE}======================================${NC}"
```

### Make Executable & Run

```bash
chmod +x test_jwt.sh
./test_jwt.sh
```

### Expected Output

```
======================================
JWT Login & GetBooks Test
======================================

Step 1: Getting JWT Token...
Username: user
Client: books-service

✅ Login successful!
Token expires in: 300 seconds
Token: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ...

Step 2: Getting Books...
✅ GetBooks successful!
Total books: 10

First 3 Books:
{
  "id": 1,
  "title": "The Great Gatsby",
  "author": "F. Scott Fitzgerald",
  "price": 12.99
}
{
  "id": 2,
  "title": "To Kill a Mockingbird",
  "author": "Harper Lee",
  "price": 14.99
}
{
  "id": 3,
  "title": "1984",
  "author": "George Orwell",
  "price": 13.99
}

======================================
✅ JWT Test Complete!
======================================
```

---

## 🔐 Other Curl Commands

### Get Specific Book

```bash
curl -X GET \
  'http://localhost:8080/api/books/1' \
  -H "Authorization: Bearer $TOKEN"
```

### Create Book

```bash
curl -X POST \
  'http://localhost:8080/api/books' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "New Book",
    "author": "Author Name",
    "price": 29.99
  }'
```

### Delete Book

```bash
curl -X DELETE \
  'http://localhost:8080/api/books/999' \
  -H "Authorization: Bearer $TOKEN"
```

### Get User Profile

```bash
curl -X GET \
  'http://localhost:8080/api/user/profile' \
  -H "Authorization: Bearer $TOKEN"
```

### Get JWT Claims

```bash
curl -X GET \
  'http://localhost:8080/api/user/claims' \
  -H "Authorization: Bearer $TOKEN"
```

### Admin Endpoint (requires admin role)

```bash
curl -X GET \
  'http://localhost:8080/api/admin/dashboard' \
  -H "Authorization: Bearer $TOKEN"
```

### Public Info (no auth needed)

```bash
curl -X GET 'http://localhost:8080/api/public/info'
```

---

## 🔄 Refresh Token

When token expires, use refresh token:

```bash
REFRESH_TOKEN=$(echo "$RESPONSE" | jq -r '.refresh_token')

NEW_RESPONSE=$(curl -s -X POST \
  'http://localhost:8180/realms/books-realm/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=refresh_token' \
  -d 'client_id=books-service' \
  -d 'client_secret=books-secret-2024' \
  -d "refresh_token=${REFRESH_TOKEN}")

NEW_TOKEN=$(echo "$NEW_RESPONSE" | jq -r '.access_token')
echo "New Token: $NEW_TOKEN"
```

---

## 🔍 Decode JWT Token

### Using jq and base64

```bash
# Decode payload (2nd part of JWT)
echo "$TOKEN" | cut -d. -f2 | base64 -d | jq .
```

### Using online tool

Visit https://jwt.io and paste your token

---

## 🧪 Admin User Test

### Login as Admin

```bash
ADMIN_TOKEN=$(curl -s -X POST \
  'http://localhost:8180/realms/books-realm/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=books-service' \
  -d 'client_secret=books-secret-2024' \
  -d 'username=admin' \
  -d 'password=admin123' \
  -d 'scope=openid profile email' | jq -r '.access_token')

echo "Admin Token: $ADMIN_TOKEN"
```

### Get Admin Dashboard

```bash
curl -X GET \
  'http://localhost:8080/api/admin/dashboard' \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## 📊 Useful jq Filters

### Pretty print with indentation

```bash
curl -s ... | jq '.'
```

### Get specific field

```bash
curl -s ... | jq '.access_token'
```

### Get array length

```bash
curl -s ... | jq 'length'
```

### Filter specific fields

```bash
curl -s ... | jq '.[] | {id, title, author}'
```

### First N items

```bash
curl -s ... | jq '.[0:3]'
```

---

## 🔗 Quick Reference

| Action | Command |
|--------|---------|
| **Login** | `curl -X POST '{url}/token' -d 'grant_type=password...'` |
| **GetBooks** | `curl -H "Authorization: Bearer $TOKEN" '{api}/api/books'` |
| **Decode JWT** | `echo $TOKEN \| cut -d. -f2 \| base64 -d \| jq` |
| **Check Expiry** | `echo $TOKEN \| cut -d. -f2 \| base64 -d \| jq .exp` |

---

## 🚨 Troubleshooting

### jq not installed

```bash
# Install jq
brew install jq          # macOS
apt-get install jq      # Linux
choco install jq        # Windows
```

### Connection Refused

```bash
# Check if services are running
docker-compose ps

# Start services
docker-compose up -d
```

### Invalid Token Error

```bash
# Login again to get fresh token
TOKEN=$(curl -s ... | jq -r '.access_token')

# Test token is valid
echo $TOKEN | jq .
```

---

## 📚 Related Guides

- [POSTMAN_JWT_LOGIN_GETBOOKS.md](POSTMAN_JWT_LOGIN_GETBOOKS.md) - Postman guide
- [POSTMAN_QUICK_START.md](POSTMAN_QUICK_START.md) - Quick Postman setup
- [POSTMAN_VISUAL_SETUP.md](POSTMAN_VISUAL_SETUP.md) - Visual steps for Postman

---

**Created**: 2026-01-25  
**Status**: ✅ Ready to use

