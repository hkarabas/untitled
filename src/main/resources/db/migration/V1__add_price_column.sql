-- Add price column to books table
ALTER TABLE books ADD COLUMN price DECIMAL(10, 2);

-- Optional: Add comment to the column
ALTER TABLE books MODIFY COLUMN price DECIMAL(10, 2) COMMENT 'Book price in decimal format';

-- Optional: Set default value for existing records
UPDATE books SET price = 0.00 WHERE price IS NULL;

