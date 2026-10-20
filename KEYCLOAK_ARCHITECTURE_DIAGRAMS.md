# Keycloak Architecture Diagrams

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Docker Network                          │
│                                                                 │
│  ┌──────────────┐       ┌──────────────┐       ┌────────────┐ │
│  │    MySQL     │       │ Keycloak     │       │   MySQL    │ │
│  │   (Books)    │       │   MySQL      │       │ (Keycloak) │ │
│  │              │       │              │       │            │ │
│  │ Port: 3306   │       │ Internal     │       │ Internal   │ │
│  │ books_db     │       │ keycloak DB  │       │ keycloak   │ │
│  └──────▲───────┘       └──────▲───────┘       └────────────┘ │
│         │                      │                               │
│         │                      │                               │
│  ┌──────┴───────┐       ┌──────┴───────┐                      │
│  │  Spring Boot │       │   Keycloak   │                      │
│  │  Books API   │◄─────►│   Server     │                      │
│  │              │       │              │                      │
│  │ Port: 8080   │       │ Port: 8180   │                      │
│  │ /api/**      │       │ /realms/**   │                      │
│  └──────▲───────┘       └──────▲───────┘                      │
│         │                      │                               │
└─────────┼──────────────────────┼───────────────────────────────┘
          │                      │
          │                      │
    ┌─────┴──────────────────────┴─────┐
    │                                  │
    │           Client                 │
    │   (Browser/Postman/cURL)         │
    │                                  │
    └──────────────────────────────────┘
```

## Authentication Flow

```
┌────────┐                ┌──────────┐              ┌──────────┐
│ Client │                │ Keycloak │              │ Books API│
└───┬────┘                └────┬─────┘              └────┬─────┘
    │                          │                         │
    │ 1. POST /token           │                         │
    │ (username + password)    │                         │
    ├─────────────────────────►│                         │
    │                          │                         │
    │                          │ 2. Validate credentials │
    │                          │    Check MySQL          │
    │                          │                         │
    │ 3. JWT Token             │                         │
    │◄─────────────────────────┤                         │
    │                          │                         │
    │ 4. GET /api/user/profile │                         │
    │    Authorization: Bearer {token}                   │
    ├────────────────────────────────────────────────────►│
    │                          │                         │
    │                          │ 5. Validate JWT         │
    │                          │    (signature, expiry)  │
    │                          │◄────────────────────────┤
    │                          │                         │
    │                          │ 6. JWT valid            │
    │                          ├────────────────────────►│
    │                          │                         │
    │                          │    7. Extract roles     │
    │                          │       Check @PreAuthorize
    │                          │                         │
    │ 8. User Profile (200 OK) │                         │
    │◄────────────────────────────────────────────────────┤
    │                          │                         │
```

## Role-Based Access Control

```
┌──────────────────────────────────────────────────────────────┐
│                     Keycloak Realm                           │
│                    (books-realm)                             │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │                  Roles                             │    │
│  │  ┌─────────┐           ┌─────────┐                │    │
│  │  │  USER   │           │  ADMIN  │                │    │
│  │  └────┬────┘           └────┬────┘                │    │
│  └───────┼──────────────────────┼─────────────────────┘    │
│          │                      │                           │
│  ┌───────┼──────────────────────┼─────────────────────┐    │
│  │       │      Users           │                     │    │
│  │  ┌────▼─────┐           ┌────▼──────┐             │    │
│  │  │   user   │           │   admin   │             │    │
│  │  │          │           │           │             │    │
│  │  │ user123  │           │ admin123  │             │    │
│  │  └──────────┘           └───────────┘             │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
└──────────────────────────────────────────────────────────────┘
                        │
                        │ JWT Token
                        ▼
┌──────────────────────────────────────────────────────────────┐
│                  Spring Boot Application                     │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │              SecurityConfig                        │    │
│  │                                                    │    │
│  │  PUBLIC      → /api/public/**  ✓ Allow All       │    │
│  │  USER        → /api/books/**   ✓ USER or ADMIN   │    │
│  │  ADMIN       → /api/admin/**   ✓ ADMIN only      │    │
│  │                                                    │    │
│  └────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
```

## JWT Token Flow

```
┌─────────────────────────────────────────────────────────────┐
│                      JWT Token                              │
│                                                             │
│  ┌──────────┐  ┌────────────────────┐  ┌───────────────┐  │
│  │  Header  │  │      Payload       │  │   Signature   │  │
│  ├──────────┤  ├────────────────────┤  ├───────────────┤  │
│  │          │  │ iss: Keycloak URL  │  │               │  │
│  │ alg:     │  │ sub: user-id       │  │   RS256       │  │
│  │  RS256   │  │ exp: 1737590400    │  │  Encrypted    │  │
│  │          │  │ iat: 1737586800    │  │   with        │  │
│  │ typ: JWT │  │ preferred_username │  │  Private Key  │  │
│  │          │  │ email: ...         │  │               │  │
│  │          │  │ realm_access:      │  │               │  │
│  │          │  │   roles: [ADMIN]   │  │               │  │
│  │          │  │ scope: openid...   │  │               │  │
│  └──────────┘  └────────────────────┘  └───────────────┘  │
│                                                             │
│      │               │                       │             │
│      ▼               ▼                       ▼             │
│   Base64         Base64                  Base64            │
│   Encoded        Encoded                 Encoded           │
└─────────────────────────────────────────────────────────────┘
         │               │                       │
         └───────────────┴───────────────────────┘
                         │
                         ▼
              eyJhbGciOiJSUzI1NiIsInR5cCI6...
```

## Request Processing Flow

```
┌──────────────────────────────────────────────────────────────┐
│                 HTTP Request Processing                      │
└──────────────────────────────────────────────────────────────┘

  Request: GET /api/user/profile
  Header: Authorization: Bearer eyJhbGc...
           │
           ▼
  ┌──────────────────────┐
  │  Security Filter     │
  │  Chain               │
  └──────┬───────────────┘
         │
         ▼
  ┌──────────────────────┐      ┌─────────────────┐
  │  JWT Authentication  │─────►│   Keycloak      │
  │  Filter              │      │   (Validate)    │
  └──────┬───────────────┘      └─────────────────┘
         │                            │
         │ ◄──────────────────────────┘
         │ Token Valid ✓
         ▼
  ┌──────────────────────┐
  │  Extract JWT Claims  │
  │  - username          │
  │  - email             │
  │  - roles             │
  └──────┬───────────────┘
         │
         ▼
  ┌──────────────────────┐
  │  Convert to          │
  │  GrantedAuthority    │
  │  ROLE_USER           │
  └──────┬───────────────┘
         │
         ▼
  ┌──────────────────────┐
  │  @PreAuthorize       │
  │  hasAnyRole('USER',  │
  │  'ADMIN')            │
  └──────┬───────────────┘
         │
         │ ✓ Authorized
         ▼
  ┌──────────────────────┐
  │  Controller Method   │
  │  userProfile(Jwt jwt)│
  └──────┬───────────────┘
         │
         ▼
  ┌──────────────────────┐
  │  Return Response     │
  │  200 OK              │
  │  { user data }       │
  └──────────────────────┘
```

## Database Schema Overview

```
┌─────────────────────────────────────────────────────────────┐
│              MySQL Instance 1 (Port 3306)                   │
│                                                             │
│  Database: books_db                                         │
│  ┌───────────────────────────────────────────────────┐    │
│  │  books                                            │    │
│  ├─────────────┬──────────────┬──────────────────────┤    │
│  │ id (PK)     │ title        │ author               │    │
│  │ isbn        │ published    │ price                │    │
│  │ ...         │              │                      │    │
│  └─────────────┴──────────────┴──────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│        Keycloak MySQL Instance (Internal)                   │
│                                                             │
│  Database: keycloak                                         │
│  ┌───────────────────────────────────────────────────┐    │
│  │  REALM                                            │    │
│  │  REALM_ROLE                                       │    │
│  │  USER_ENTITY                                      │    │
│  │  USER_ROLE_MAPPING                                │    │
│  │  CLIENT                                           │    │
│  │  CLIENT_SESSION                                   │    │
│  │  ... (60+ Keycloak tables)                        │    │
│  └───────────────────────────────────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Endpoint Access Matrix

```
┌──────────────────────────────────────────────────────────────┐
│                    Endpoint Access Matrix                    │
├────────────────────────┬───────────┬──────────┬──────────────┤
│ Endpoint               │ Anonymous │   USER   │    ADMIN     │
├────────────────────────┼───────────┼──────────┼──────────────┤
│ /api/public/info       │     ✓     │    ✓     │      ✓       │
│ /api/health            │     ✓     │    ✓     │      ✓       │
│ /swagger-ui/**         │     ✓     │    ✓     │      ✓       │
│ /v3/api-docs/**        │     ✓     │    ✓     │      ✓       │
├────────────────────────┼───────────┼──────────┼──────────────┤
│ /api/user/profile      │     ✗     │    ✓     │      ✓       │
│ /api/user/claims       │     ✗     │    ✓     │      ✓       │
│ /api/books             │     ✗     │    ✓     │      ✓       │
│ /api/books/{id}        │     ✗     │    ✓     │      ✓       │
├────────────────────────┼───────────┼──────────┼──────────────┤
│ /api/admin/dashboard   │     ✗     │    ✗     │      ✓       │
│ /api/admin/users       │     ✗     │    ✗     │      ✓       │
└────────────────────────┴───────────┴──────────┴──────────────┘

Legend:
  ✓ = Access Granted (200 OK)
  ✗ = Access Denied (401/403)
```

## Token Lifecycle

```
┌────────────────────────────────────────────────────────────┐
│                    Token Lifecycle                         │
└────────────────────────────────────────────────────────────┘

1. TOKEN REQUEST
   ┌──────────┐
   │  Client  │
   └────┬─────┘
        │ POST /token
        │ username + password
        ▼
   ┌─────────────┐
   │  Keycloak   │
   └────┬────────┘
        │
        ▼

2. TOKEN ISSUED
   ┌─────────────────────────┐
   │ Access Token            │
   │ - Lifetime: 3600s (1h)  │
   │ - Type: JWT             │
   │ - Algorithm: RS256      │
   └─────────────────────────┘
   ┌─────────────────────────┐
   │ Refresh Token           │
   │ - Lifetime: 1800s (30m) │
   │ - Can get new access    │
   └─────────────────────────┘

3. TOKEN USAGE
   Time: 0s ────────► 3600s ────────► 3601s
          │              │              │
          ✓ Valid        ✓ Valid        ✗ Expired
          200 OK         200 OK         401 Unauthorized

4. TOKEN REFRESH (Optional)
   ┌──────────┐
   │  Client  │ POST /token
   └────┬─────┘ grant_type=refresh_token
        │       refresh_token=...
        ▼
   ┌─────────────┐
   │  Keycloak   │ New Access Token
   └────┬────────┘ (Reset 3600s timer)
        │
        ▼
   New Access Token Issued
```

## Component Interaction

```
┌──────────────────────────────────────────────────────────┐
│              Spring Boot Layers                          │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │  Controller Layer                              │    │
│  │  - @RestController                             │    │
│  │  - @PreAuthorize                               │    │
│  │  - AuthController                              │    │
│  │  - BookController                              │    │
│  └─────────────────┬──────────────────────────────┘    │
│                    │                                     │
│  ┌─────────────────▼──────────────────────────────┐    │
│  │  Security Layer                                │    │
│  │  - SecurityConfig                              │    │
│  │  - SecurityFilterChain                         │    │
│  │  - JwtAuthenticationConverter                  │    │
│  └─────────────────┬──────────────────────────────┘    │
│                    │                                     │
│  ┌─────────────────▼──────────────────────────────┐    │
│  │  Service Layer                                 │    │
│  │  - BookService                                 │    │
│  │  - Business Logic                              │    │
│  └─────────────────┬──────────────────────────────┘    │
│                    │                                     │
│  ┌─────────────────▼──────────────────────────────┐    │
│  │  Repository Layer                              │    │
│  │  - JPA Repositories                            │    │
│  │  - Database Access                             │    │
│  └─────────────────┬──────────────────────────────┘    │
│                    │                                     │
└────────────────────┼─────────────────────────────────────┘
                     │
                     ▼
              ┌─────────────┐
              │    MySQL    │
              │   books_db  │
              └─────────────┘
```

---

**Visual Reference Complete** 📊

These diagrams illustrate the complete Keycloak integration architecture.

