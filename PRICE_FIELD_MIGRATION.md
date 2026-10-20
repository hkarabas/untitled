# Adding Price Field to Books Table

## Changes Made

### 1. Entity Changes
- Added `price` field to `Book.java` entity
- Type: `BigDecimal` with precision 10 and scale 2
- Added constructor with price parameter
- Added getter and setter methods for price

### 2. Database Migration

#### Option A: Automatic Migration (Hibernate)
The application is configured with `spring.jpa.hibernate.ddl-auto=update`, so the price column will be automatically added when you restart the application.

#### Option B: Manual MySQL ALTER TABLE

You can manually execute the ALTER TABLE statement by running:

```bash
mysql -u books_user -p books_db < alter_books_table.sql
```

Or execute directly in MySQL:

```sql
USE books_db;

-- Add price column
ALTER TABLE books 
ADD COLUMN price DECIMAL(10, 2) NULL;

-- Verify the change
DESCRIBE books;

-- Optional: Set random prices for existing books
UPDATE books SET price = ROUND(RAND() * 94.00 + 5.99, 2) WHERE price IS NULL;
```

### 3. Service Layer Changes
- Updated `BookServiceImpl.java` to generate random prices between $5.99 and $99.99 for new books
- Added imports for `BigDecimal` and `RoundingMode`

### 4. Generated Files

1. **alter_books_table.sql** - Standalone SQL script for manual execution
2. **src/main/resources/db/migration/V1__add_price_column.sql** - Migration script (for Flyway if you add it later)

## Testing the Changes

### 1. Restart the application:
```bash
mvn spring-boot:run
```

### 2. Create a new book with price:
```bash
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Book",
    "author": "Test Author",
    "price": 29.99
  }'
```

### 3. Generate random books (they will now include prices):
```bash
curl -X POST "http://localhost:8080/api/books/generate-random?count=1000"
```

### 4. Retrieve books to verify price field:
```bash
curl http://localhost:8080/api/books
```

## Database Schema After Changes

```sql
CREATE TABLE books (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  author VARCHAR(255) NOT NULL,
  price DECIMAL(10, 2),
  INDEX idx_books_price (price)  -- Optional index for price-based queries
);
```

## Notes

- The price field is nullable to support existing records
- Prices are stored with 2 decimal places (e.g., 29.99)
- Random book generation now includes prices between $5.99 and $99.99
- The BigDecimal type ensures precise decimal calculations without floating-point errors

