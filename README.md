# Spring Boot + MySQL (Docker) Demo

Layers: controller / service / repository with JPA. Domain: Book.

**⚡ Now with Java 21 Virtual Threads for parallel batch processing!**

## Prereqs
- JDK 25
- Docker + Docker Compose

## Run locally (dev, hits local MySQL or change URL)
```bash
./mvnw spring-boot:run
```

## Run everything with Docker
```bash
docker compose up --build
```
The `app` service waits for MySQL health check, uses env overrides for datasource.

## REST API Endpoints

### Standard CRUD Operations
- `GET  http://localhost:8080/api/books` - Get all books
- `POST http://localhost:8080/api/books` - Create book (JSON: `{ "title": "Foo", "author": "Bar" }`)
- `GET  http://localhost:8080/api/books/{id}` - Get book by ID
- `DELETE http://localhost:8080/api/books/{id}` - Delete book

### 🚀 Virtual Threads Feature (Java 21)
- `POST http://localhost:8080/api/books/generate-random?count=100000` - Generate random books

**Features:**
- Uses Java 21 virtual threads for parallel batch processing
- Generates 100,000 books (default) or specify custom count
- 2-5x faster than sequential processing
- Progress logging to console
- Example: `curl -X POST "http://localhost:8080/api/books/generate-random?count=100000"`

## Config
- Application config: `src/main/resources/application.yml`
- Docker compose: `docker-compose.yml`

## Documentation
- 📖 [Virtual Threads Implementation](VIRTUAL_THREADS_IMPLEMENTATION.md) - Technical details
- 📖 [Random Books Usage Guide](RANDOM_BOOKS_USAGE.md) - Complete usage instructions

## Notes
- Default credentials: user `books_user` / `books_pass`, DB `books_db`.
- Switch to H2 for tests via test scope dependency.
- Java 25 is the configured runtime; virtual threads were introduced in Java 21.
