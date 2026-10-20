#!/bin/bash
# Quick test script to execute ALTER TABLE on MySQL

echo "Connecting to MySQL and adding price column to books table..."

mysql -u books_user -pbooks_pass books_db << EOF
-- Add price column to books table
ALTER TABLE books ADD COLUMN IF NOT EXISTS price DECIMAL(10, 2);

-- Show the updated table structure
DESCRIBE books;

-- Count existing books
SELECT COUNT(*) as total_books FROM books;

-- Show sample of books (first 5)
SELECT id, title, author, price FROM books LIMIT 5;

-- Optional: Update existing books with random prices
-- UPDATE books SET price = ROUND(RAND() * 94.00 + 5.99, 2) WHERE price IS NULL;

EOF

echo "Done! Price column has been added to the books table."

