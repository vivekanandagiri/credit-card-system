
-- Add timezone column (safe, idempotent)

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'customers'
          AND table_schema = 'public'
          AND column_name = 'timezone'
    ) THEN
        ALTER TABLE customers
        ADD COLUMN timezone VARCHAR(50);
    END IF;
END $$;


-- Backfill existing records 

UPDATE customers
SET timezone = 'UTC'
WHERE timezone IS NULL;


-- Set DEFAULT for future inserts

ALTER TABLE customers
ALTER COLUMN timezone SET DEFAULT 'UTC';


-- Set NOT NULL constraint

ALTER TABLE customers
ALTER COLUMN timezone SET NOT NULL;


-- Add check constraint (non-empty, safe)

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'customers'
          AND constraint_name = 'chk_customers_timezone_not_empty'
    ) THEN
        ALTER TABLE customers
        ADD CONSTRAINT chk_customers_timezone_not_empty
        CHECK (trim(timezone) <> '');
    END IF;
END $$;


-- Index (only if filter by timezone)

CREATE INDEX IF NOT EXISTS idx_customers_timezone
ON customers(timezone);