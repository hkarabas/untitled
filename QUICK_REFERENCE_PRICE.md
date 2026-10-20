# Quick Reference: Add Price Column to Books Table

## 🎯 One-Line Solutions

### MySQL Command Line
```sql
ALTER TABLE books ADD COLUMN price DECIMAL(10, 2);
```

### Using Script
```bash
./add_price_column.sh
```

### Using MySQL Client
```bash
mysql -u books_user -pbooks_pass books_db -e "ALTER TABLE books ADD COLUMN price DECIMAL(10, 2); DESCRIBE books;"
```

## 📋 Column Details

| Property | Value |
|----------|-------|
| Column Name | `price` |
| Data Type | `DECIMAL(10, 2)` |
| Nullable | YES |
| Default | NULL |
| Java Type | `java.math.BigDecimal` |

## ✅ Verification Commands

```sql
-- Check if column exists
SHOW COLUMNS FROM books LIKE 'price';

-- View table structure
DESCRIBE books;

-- Count books with prices
SELECT COUNT(*) FROM books WHERE price IS NOT NULL;
```

## 🔄 After Adding Column

```sql
-- Update existing books with random prices
UPDATE books SET price = ROUND(RAND() * 94.00 + 5.99, 2) WHERE price IS NULL;

-- Verify prices were set
SELECT id, title, price FROM books LIMIT 10;
```

## 🚀 Restart Application
```bash
mvn spring-boot:run
```

That's it! 🎉

