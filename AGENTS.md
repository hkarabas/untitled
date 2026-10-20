# AGENTS.md – AI Coding Agent Guide

## Project Overview
Spring Boot 4 / Java 25 REST API for managing `Book` entities backed by MySQL. Primary focus is **high-throughput bulk ingestion** via CSV using multiple parallel strategies. Keycloak handles OAuth2/JWT auth.

## Architecture
```
BookController  →  BookService (interface)  →  BookServiceImpl  →  BookRepository (JPA, single-row CRUD)
                                                                  └→ BookBatchWriter (JdbcTemplate, bulk inserts)
                                                                  └→ MySQL (via HikariCP, pool=30)
```
- `CsvBookParser` is the single CSV line parser shared by all import strategies
- Single domain entity: `Book` (id, title, author, `BigDecimal` price)
- Config classes: `SecurityConfig`, `CorsConfig`, `OpenApiConfig`
- `GlobalExceptionHandler` handles cross-cutting error responses

## Key Files
| File | Purpose |
|------|---------|
| `src/main/java/com/example/untitled/service/impl/BookServiceImpl.java` | All bulk import logic |
| `src/main/java/com/example/untitled/config/SecurityConfig.java` | Keycloak JWT role mapping |
| `src/main/resources/application.yml` | All config (DB, Keycloak, upload limits) |
| `docker-compose.yml` | Full stack: MySQL + Keycloak + app |

## Developer Workflows

### Run full stack (Docker)
```bash
docker compose up --build
# App: http://localhost:8080  |  Keycloak: http://localhost:8180
# Remote debug port: 5005 on 127.0.0.1 only (enabled via JAVA_TOOL_OPTIONS in compose; the image itself has no debugger)
```

### Run locally (requires local MySQL)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Tests
```bash
mvn test   # unit tests + @WebMvcTest security tests + @DataJpaTest on H2; no MySQL/Keycloak needed
```

### Swagger UI
```
http://localhost:8080/swagger-ui.html  (public, no auth required)
```

## Authentication Pattern
- All `/api/books/**` endpoints require JWT bearer token with role `USER` or `ADMIN`
- The app is a stateless **resource server only** (no `oauth2Login`, no client secret); get tokens from Keycloak directly (see `jwt_test.sh`)
- `SecurityConfig` extracts roles from **both** `realm_access.roles` and `resource_access.<client>.roles` JWT claims, prefixing with `ROLE_`
- Public endpoints: `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`, `/api/public/**`, `/api/health`, `/`, `/health`, `/info`, `/error`

## CSV Import Strategies (4 variants)
All accept `multipart/form-data` with field name `file`. Max upload: **200MB**.

| Endpoint | Method | Behaviour |
|----------|--------|-----------|
| `/api/books/import-csv-traditional` | Synchronous, single-threaded | Baseline |
| `/api/books/import-csv-virtual-threads` | Synchronous, streaming: one virtual thread per 5000-line batch, max 20 in flight (semaphore backpressure) | Faster, bounded memory |
| `/api/books/import-csv-parallel-fire-and-forget` | **Returns 202 immediately**, background virtual threads | Non-blocking |
| `/api/books/import-csv-parallel-stream` | Synchronous, `parallelStream()` via ForkJoinPool | CPU-bound |

CSV format: `title,author,price`. Quoted fields and escaped quotes (`""`) supported; a header row or any malformed line is skipped and counted in the response.

## Batch Processing Conventions
- Batch size: **5000 records** per `BookBatchWriter.insert()` call — a JDBC `batchUpdate`, one transaction per batch. Hibernate is bypassed because `IDENTITY` ids disable Hibernate insert batching
- Only **one bulk operation** (any import or `generate-random`) runs at a time; a concurrent request gets **429**
- `generate-random` accepts `count` 1..1,000,000
- Concurrent strategies go through `runConcurrently()`: a fixed pool of 20 virtual threads (below the Hikari pool of 30). If any batch fails, the rest are cancelled and the request fails with 500 — batches already committed stay committed
- Fire-and-forget uses `Thread.ofVirtual().start(...)`; it holds the bulk permit and deletes its temp file in a `finally` block. Its outcome is only logged

## Database Notes
- `ddl-auto: update` — schema evolves automatically; no migration tool
- `rewriteBatchedStatements=true` in JDBC URL is critical for batch insert performance
- Docker Keycloak uses a **separate** MySQL instance (`keycloak-mysql`) on the same `app-network`
- Issuer URI is always `http://localhost:8180/realms/books-realm` (the URL in the token); in Docker only the JWK set is fetched via `http://keycloak:8080/...`
- Prod profile requires `KEYCLOAK_ISSUER_URI` and `KEYCLOAK_JWK_SET_URI` env vars

## Project-Specific Conventions
- `Book` has no Lombok — use explicit getters/setters and all-args constructor
- Use `java.lang.String.format(...)` for explicit locale-independent formatting when adding formatted output.
- `price` field is `BigDecimal`, always set to 2 decimal places via `RoundingMode.HALF_UP`
- Controllers return `ResponseEntity` for all non-list endpoints

