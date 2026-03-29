--Drop FK constraint on customer_id
ALTER TABLE cards
DROP CONSTRAINT IF EXISTS fk_card_customer;

--Drop index (if exists)
DROP INDEX IF EXISTS idx_card_customer;

--Drop column customer_id
ALTER TABLE cards
DROP COLUMN IF EXISTS customer_id;

-- Add card-level control flags
ALTER TABLE cards
ADD COLUMN IF NOT EXISTS online_enabled BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS atm_enabled BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS international_enabled BOOLEAN NOT NULL DEFAULT FALSE;

--Backfill for safety
UPDATE cards
SET online_enabled = TRUE
WHERE online_enabled IS NULL;

UPDATE cards
SET atm_enabled = TRUE
WHERE atm_enabled IS NULL;

UPDATE cards
SET international_enabled = FALSE
WHERE international_enabled IS NULL;

-- Add indexes 
CREATE INDEX IF NOT EXISTS idx_card_online_enabled ON cards(online_enabled);
CREATE INDEX IF NOT EXISTS idx_card_atm_enabled ON cards(atm_enabled);
CREATE INDEX IF NOT EXISTS idx_card_international_enabled ON cards(international_enabled);