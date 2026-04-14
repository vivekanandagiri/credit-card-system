-- ============================================
-- Rebuild statement_status_enum
-- Old:
-- GENERATED, DUE, PAID, PARTIALLY_PAID, OVERDUE
--
-- New:
-- GENERATED, PAID, REVOLVING, OVERDUE
-- ============================================

CREATE TYPE statement_status_enum_new AS ENUM (
    'GENERATED',
    'PAID',
    'REVOLVING',
    'OVERDUE'
);

-- IMPORTANT: Drop old default before enum conversion
ALTER TABLE billing_statements
ALTER COLUMN statement_status DROP DEFAULT;

ALTER TABLE billing_statements
ALTER COLUMN statement_status
TYPE statement_status_enum_new
USING (
    CASE statement_status::text
        WHEN 'PARTIALLY_PAID' THEN 'REVOLVING'
        WHEN 'DUE' THEN 'GENERATED'
        ELSE statement_status::text
    END
)::statement_status_enum_new;

DROP TYPE statement_status_enum;

ALTER TYPE statement_status_enum_new
RENAME TO statement_status_enum;

-- Re-add default using new enum
ALTER TABLE billing_statements
ALTER COLUMN statement_status
SET DEFAULT 'GENERATED';


-- ============================================
-- Remove obsolete paid_at
-- ============================================

ALTER TABLE billing_statements
DROP COLUMN paid_at;


-- ============================================
-- Add late fee tracking
-- ============================================

ALTER TABLE billing_statements
ADD COLUMN late_fee_applied BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN late_fee_applied_at TIMESTAMP WITH TIME ZONE;


-- ============================================
-- Helpful Indexes
-- ============================================

CREATE INDEX idx_billing_statement_due_status
ON billing_statements(due_date, statement_status);