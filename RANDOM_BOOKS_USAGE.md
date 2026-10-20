# Random Books Generator - Usage Guide

## Overview
The application now includes a feature to generate and insert random books into the database. This is useful for testing, performance benchmarking, and populating the database with sample data.

## Implementation Details

### Service Layer
- **Interface**: `BookService.generateRandomBooks(int count)`
- **Implementation**: `BookServiceImpl.generateRandomBooks(int count)`
- **Transaction Management**: Uses `@Transactional` annotation for proper database transaction handling
- **Batch Processing**: Saves books in batches of 1,000 to optimize memory usage and database performance

### Data Generation
The random books are created with:
- **Authors**: Combination of 15 first names and 14 last names (210 possible combinations)
- **Titles**: Generated from 14 adjectives and 15 nouns with sequential numbering
- **Example**: "The Ancient Adventure 1", "The Mysterious Quest 2", etc.

## API Endpoint

### Generate Random Books
```http
POST /api/books/generate-random?count=100000
```

**Parameters:**
- `count` (optional, default: 100000): Number of random books to generate

**Response:**
```json
"Successfully generated 100000 random books in 45230 ms (45.23 seconds)"
```

**Response Status:** 200 OK

## Usage Examples

### 1. Generate Default 100,000 Books
```bash
curl -X POST http://localhost:8080/api/books/generate-random
```

### 2. Generate Custom Amount (e.g., 1,000 Books)
```bash
curl -X POST "http://localhost:8080/api/books/generate-random?count=1000"
```

### 3. Generate 100,000 Books (Explicit)
```bash
curl -X POST "http://localhost:8080/api/books/generate-random?count=100000"
```

### 4. Using HTTPie (Alternative)
```bash
http POST http://localhost:8080/api/books/generate-random count==100000
```

### 5. Using Postman
1. Create new POST request
2. URL: `http://localhost:8080/api/books/generate-random`
3. Add query parameter: `count` = `100000`
4. Send request

## Running the Application

### Start with Docker Compose
```bash
# Start MySQL and the application
docker-compose up

# Or run in detached mode
docker-compose up -d
```

### Wait for Services
The application will:
1. Wait for MySQL to be healthy (via health check)
2. Start the Spring Boot application
3. Create database tables automatically (via JPA)
4. Be ready to accept requests on port 8080

### Check if Application is Ready
```bash
# Check if the API is responding
curl http://localhost:8080/api/books

# Expected response (initially empty):
[]
```

## Complete Workflow Example

```bash
# 1. Start the services
docker-compose up -d

# 2. Wait a few seconds for startup
sleep 10

# 3. Generate 100,000 random books
curl -X POST "http://localhost:8080/api/books/generate-random?count=100000"

# 4. Verify books were created (get first 10)
curl "http://localhost:8080/api/books" | head -n 50

# 5. Get a specific book by ID
curl http://localhost:8080/api/books/1
```

## Performance Considerations

### Batch Size
The implementation uses batches of 1,000 books to:
- Reduce memory consumption
- Optimize database insert performance
- Prevent out-of-memory errors

### Expected Performance
- **100,000 records**: Approximately 30-60 seconds (depends on hardware)
- **Database**: MySQL 8.0 with default configuration
- **Memory**: Constant memory usage due to batch processing

### Database Configuration
For better performance with large inserts, you can optimize MySQL configuration in `docker-compose.yml`:

```yaml
mysql:
  environment:
    # ... existing config ...
  command:
    - --innodb_buffer_pool_size=1G
    - --max_allowed_packet=256M
    - --innodb_log_file_size=512M
```

## Troubleshooting

### Application Not Starting
```bash
# Check logs
docker-compose logs app

# Check MySQL health
docker-compose logs mysql
```

### Slow Performance
- Check available system resources (RAM, CPU)
- Verify MySQL is using proper configuration
- Consider reducing batch size if memory is limited

### Connection Issues
```bash
# Verify services are running
docker-compose ps

# Restart services
docker-compose restart
```

## Database Schema

The books table structure:
```sql
CREATE TABLE books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL
);
```

## Additional API Endpoints

### Get All Books
```bash
curl http://localhost:8080/api/books
```

### Get Book by ID
```bash
curl http://localhost:8080/api/books/1
```

### Create Single Book
```bash
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"My Book","author":"John Doe"}'
```

### Delete Book
```bash
curl -X DELETE http://localhost:8080/api/books/1
```

## Clean Up

### Stop Services
```bash
docker-compose down
```

### Remove Data Volume (Complete Reset)
```bash
docker-compose down -v
```

This will delete all data in the MySQL database.

## Notes

- The generated data is random but follows consistent patterns
- Each book gets a unique auto-incremented ID
- The operation is transactional - if it fails, no partial data is saved
- Progress is tracked and reported in the response with timing information

