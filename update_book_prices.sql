-- Update existing books with random prices
-- This script sets prices between $5.99 and $99.99 for books that don't have a price

USE books_db;

-- Update books with NULL prices
UPDATE books
SET price = ROUND(RAND() * 94.00 + 5.99, 2)
WHERE price IS NULL;

-- Show statistics
SELECT
    COUNT(*) as total_books,
    MIN(price) as min_price,
    MAX(price) as max_price,
    AVG(price) as avg_price,
    COUNT(CASE WHEN price IS NULL THEN 1 END) as books_without_price
FROM books;

-- Show sample of books with prices
SELECT id, title, author, price
FROM books
WHERE price IS NOT NULL
ORDER BY price DESC
LIMIT 10;

