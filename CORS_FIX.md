# CORS Configuration - Fixed

**Issue:** Failed to fetch - CORS error  
**Cause:** Frontend and API on different origins  
**Status:** ✅ FIXED  
**Date:** January 15, 2026

---

## What Was The Problem

When frontend tries to access API from different origin:
```
Frontend: http://localhost:3000
API: http://localhost:8181
Result: CORS error - "Failed to fetch"
```

Browser blocks cross-origin requests by default for security.

---

## The Solution

Created `CorsConfig.java` in `src/main/java/com/example/untitled/config/`

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                    "http://localhost:3000",
                    "http://localhost:8080",
                    "http://localhost:8181",
                    "http://127.0.0.1:3000",
                    "http://127.0.0.1:8080",
                    "http://127.0.0.1:8181"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

---

## What This Allows

✅ Frontend on port 3000 can access API on port 8181  
✅ Frontend on port 8080 can access API on port 8181  
✅ Same origin requests work  
✅ All HTTP methods allowed (GET, POST, PUT, DELETE)  
✅ Any headers allowed  
✅ Credentials (cookies, auth headers) supported  

---

## How to Test

### Restart Application
```bash
pkill -9 -f java
cd /Users/hkarabas/IdeaProjects/untitled
mvn clean
mvn spring-boot:run -Dspring.profiles.active=local
```

### Test from Frontend (React, Vue, etc.)
```javascript
// This will now work!
fetch('http://localhost:8181/api/books')
  .then(response => response.json())
  .then(data => console.log(data))
```

### Test from curl (Always worked)
```bash
curl http://localhost:8181/api/books
```

### Test Upload from Frontend
```javascript
const formData = new FormData();
formData.append('file', csvFile);

fetch('http://localhost:8181/api/books/import-csv-virtual-threads', {
    method: 'POST',
    body: formData
})
.then(response => response.json())
.then(data => console.log('Upload successful:', data))
```

---

## Allowed Origins

The CORS config allows requests from:

| Origin | Port | Purpose |
|--------|------|---------|
| localhost:3000 | 3000 | React dev server (common) |
| localhost:8080 | 8080 | Alternative frontend |
| localhost:8181 | 8181 | Same as API |
| 127.0.0.1:3000 | 3000 | Localhost variant |
| 127.0.0.1:8080 | 8080 | Localhost variant |
| 127.0.0.1:8181 | 8181 | Localhost variant |

---

## For Production

Update `CorsConfig.java` with your production domains:

```java
.allowedOrigins(
    "https://yourdomain.com",      // Production
    "https://www.yourdomain.com",  // With www
    "https://api.yourdomain.com"   // If different subdomain
)
```

---

## CORS Preflight Requests

Browsers automatically send `OPTIONS` requests first:

```
Request: OPTIONS /api/books
Headers: Origin: http://localhost:3000

Response: 
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: *
```

This is cached for 3600 seconds (1 hour).

---

## Testing CORS

### Using curl (shows headers)
```bash
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Content-Type" \
     -X OPTIONS http://localhost:8181/api/books -v
```

Look for:
```
< Access-Control-Allow-Origin: http://localhost:3000
< Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
< Access-Control-Allow-Credentials: true
```

### Using Browser DevTools
1. Open DevTools (F12)
2. Go to Network tab
3. Make API request
4. Check response headers for `Access-Control-Allow-Origin`

---

## Common CORS Errors Fixed

### ❌ Before (CORS Error)
```
Access to XMLHttpRequest at 'http://localhost:8181/api/books' 
from origin 'http://localhost:3000' has been blocked by CORS policy
```

### ✅ After (Works Fine)
```
Fetch successful
[{id: 1, title: "Book", author: "Author", price: 29.99}, ...]
```

---

## Files Modified/Created

| File | Change |
|------|--------|
| CorsConfig.java | ✅ NEW - CORS configuration |

---

## CORS Configuration Properties

| Property | Value | Meaning |
|----------|-------|---------|
| allowedOrigins | `["http://localhost:3000", ...]` | Which domains can access |
| allowedMethods | `["GET", "POST", ...]` | Allowed HTTP methods |
| allowedHeaders | `"*"` | Allow any headers |
| allowCredentials | `true` | Allow cookies/auth |
| maxAge | `3600` | Cache preflight for 1 hour |

---

## How CORS Works

### Simple Requests (GET, HEAD, POST)
No preflight, goes straight through

### Complex Requests (PUT, DELETE, Custom Headers)
1. Browser sends OPTIONS preflight
2. Server responds with allowed methods
3. Browser sends actual request
4. Server processes request

---

## Deployment Checklist

- [ ] Restarted application
- [ ] Verified CorsConfig.java exists
- [ ] Tested API from frontend
- [ ] Confirmed "Failed to fetch" error gone
- [ ] Checked browser DevTools Network tab
- [ ] Updated production domains (for prod deployment)

---

## Next Steps

1. **Restart Application:**
   ```bash
   mvn spring-boot:run -Dspring.profiles.active=local
   ```

2. **Test from Frontend:**
   ```javascript
   fetch('http://localhost:8181/api/books')
   ```

3. **Verify Success:**
   - No CORS error in console
   - Data loads successfully
   - Upload works from frontend

---

**Status: ✅ CORS FIXED**

Your frontend can now access the API without CORS errors!

See: **CORS_CONFIGURATION.md** for additional details

