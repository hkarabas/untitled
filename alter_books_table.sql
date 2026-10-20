-- ====================================
-- ALTER TABLE Script for books table
-- Add price column
-- ====================================

USE books_db;

-- Add price column
ALTER TABLE books
ADD COLUMN price DECIMAL(10, 2) NULL;

-- Optional: Add index on price if you plan to query by price range
-- CREATE INDEX idx_books_price ON books(price);

-- Optional: Update existing records with default price
-- UPDATE books SET price = 9.99 WHERE price IS NULL;

-- Verify the change
DESCRIBE books;

-- Sample update to set prices for existing books
-- UPDATE books SET price = ROUND(RAND() * 100, 2) WHERE price IS NULL;

