# AGENTS.md – AI Coding Agent Guide

## Project Overview
Spring Boot 4 / Java 25 REST API for managing `Book` entities backed by MySQL. Primary focus is **high-throughput bulk ingestion** from CSV and Excel (.xlsx) files using multiple parallel strategies. Keycloak handles OAuth2/JWT auth.

## Architecture
```
BookController → BookService → BookServiceImpl → BookRepository (JPA, single-row CRUD)
                                              └→ BookBatchWriter (JdbcTemplate, bulk inserts) → MySQL
CsvImportController  ─┐ (both extend AbstractImportController, which fixes the accepted ImportFormat)
ExcelImportController ┴→ BookImportService (facade, package service.importer)
                           ├→ ImportStrategy beans: traditional | virtual-threads | parallel-stream
                           └→ BookReaderFactory → CsvBookReader | ExcelBookReader
```
Design patterns in `service.importer`:
| Pattern | Class(es) | Role |
|---|---|---|
| Strategy | `ImportStrategy` + 3 `@Component` implementations | How records become DB rows; looked up by `name()` |
| Template Method | `AbstractImportStrategy.run()` | Shared steps (open reader, time, log, close); subclasses implement `doImport()` |
| Factory | `BookReaderFactory` | Picks the reader for an `ImportFormat` |
| Iterator | `BookReader.next()` / `BookRecord` | Strategies read format-agnostic records one at a time |
| Facade | `BookImportService` | Validation, `BulkOperationGuard` permit, temp file, sync vs background |

- **New strategy:** add a `@Component` extending `AbstractImportStrategy`; it is picked up automatically. **New format:** add an `ImportFormat` constant, a `BookReader`, and a case in `BookReaderFactory`
- `ImportJob` owns the temp copy of the upload and the permit; whoever runs it closes it (so background imports clean up themselves)
- `BookFieldMapper` holds the single validation rule set (title/author non-empty, numeric price) used by both readers
- Single domain entity: `Book` (id, title, author, `BigDecimal` price)
- Config classes: `SecurityConfig`, `CorsConfig`, `OpenApiConfig`
- `GlobalExceptionHandler` handles cross-cutting error responses

## Key Files
| File | Purpose |
|------|---------|
| `src/main/java/com/example/untitled/service/importer/` | File import: facade, strategies, readers |
| `src/main/java/com/example/untitled/service/impl/BookServiceImpl.java` | CRUD + random generation |
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
mvn verify # also runs *IT integration tests (Failsafe) against MySQL 8.0 in Docker via Testcontainers
```
`BookInsertIT` inserts 100,000 rows with every strategy × format (CSV, XLSX), plus background imports and generation, and checks the table contents (row count, no duplicates, price sum, spot rows). `support/XlsxTestFiles` builds .xlsx files in memory for tests.

### Swagger UI
```
http://localhost:8080/swagger-ui.html  (public, no auth required)
```

## Authentication Pattern
- All `/api/books/**` endpoints require JWT bearer token with role `USER` or `ADMIN`
- The app is a stateless **resource server only** (no `oauth2Login`, no client secret); get tokens from Keycloak directly (see `jwt_test.sh`)
- `SecurityConfig` extracts roles from **both** `realm_access.roles` and `resource_access.<client>.roles` JWT claims, prefixing with `ROLE_`
- Public endpoints: `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`, `/api/public/**`, `/api/health`, `/`, `/health`, `/info`, `/error`

## File Import (CSV and Excel)
One endpoint per format (`multipart/form-data`, field `file`); each rejects other file types with **400**. Max upload: **200MB**.

| Endpoint | Accepts | Controller / Swagger group |
|---|---|---|
| `POST /api/books/import/csv?strategy=<name>&async=<bool>` | `.csv` only | `CsvImportController` / "CSV Import" |
| `POST /api/books/import/excel?strategy=<name>&async=<bool>` | `.xlsx` only | `ExcelImportController` / "Excel Import" |

Default strategy `virtual-threads`; `async=true` returns **202** and runs in the background (outcome only logged, prefix `[bg]`).

| Strategy | Behaviour | Notes |
|----------|-----------|-------|
| `traditional` | Single-threaded read → parse → insert | Baseline |
| `virtual-threads` | Streaming: one virtual thread per 5000-record batch, max 20 in flight (semaphore backpressure); logs `[vt]` | Fastest, bounded memory |
| `parallel-stream` | Reads all records, `parallelStream()` parsing in a dedicated ForkJoinPool (`app.import.parallel-stream.parallelism`, 0 = CPU cores), then sequential inserts; logs `[ps]` | Insert-bound (~95% of time) |

Shortcut endpoints with a fixed strategy, the same set for both formats:

| CSV (`.csv` only) | Excel (`.xlsx` only) | Same as |
|---|---|---|
| `/import-csv-traditional` | `/import-excel-traditional` | `strategy=traditional` |
| `/import-csv-virtual-threads` | `/import-excel-virtual-threads` | `strategy=virtual-threads` |
| `/import-csv-parallel-stream` | `/import-excel-parallel-stream` | `strategy=parallel-stream` |
| `/import-csv-parallel-fire-and-forget` | `/import-excel-parallel-fire-and-forget` | `strategy=virtual-threads&async=true` |

### Random sample files
`GET /api/books/sample/csv?rows=N` and `GET /api/books/sample/excel?rows=N` (default 100,000, max 1,000,000; requires USER) download a random books file — header row + `title, author, price`, unique titles, prices 5.99–99.99 — ready to upload to the import endpoints. Generated by `SampleFileService` with the same data as `generate-random` (`RandomBooks`); streamed via `StreamingResponseBody`, Excel through POI's `SXSSFWorkbook` (1000-row window), so memory stays flat. 1M rows: CSV ~0.2 s / 47 MB, Excel ~4 s / 23 MB. `spring.mvc.async.request-timeout` is 5m so slow downloads are not cut off.

Formats (columns `title, author, price`; a header row or any invalid record is skipped and counted):
- **CSV**: UTF-8; quoted fields and escaped quotes (`""`) supported
- **XLSX**: first sheet only, columns A–C, read with a streaming parser (excel-streaming-reader on Apache POI), so large sheets are not loaded into memory. Numeric and text cells both work; formulas use their cached value. Old binary `.xls` is not supported
- Excel parsing is ~2–3× slower than CSV (XML decoding on the reading thread)

## Batch Processing Conventions
- Batch size: **5000 records** per `BookBatchWriter.insert()` call — a JDBC `batchUpdate`, one transaction per batch. Hibernate is bypassed because `IDENTITY` ids disable Hibernate insert batching
- Only **one bulk operation** (any import or `generate-random`) runs at a time, enforced by `BulkOperationGuard`; a concurrent request gets **429**
- `generate-random` accepts `count` 1..1,000,000; it uses `runConcurrently()` in `BookServiceImpl` (fixed pool of 20 virtual threads)
- At most 20 concurrent DB writers (below the Hikari pool of 30). If any batch fails, the import stops and the request fails with 500 — batches already committed stay committed
- Background imports run on `Thread.ofVirtual()`; the `ImportJob` they own releases the permit and deletes the temp file when closed

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

