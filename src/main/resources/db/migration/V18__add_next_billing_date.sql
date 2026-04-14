
-- CREDIT ACCOUNT BILLING FLOW OVERVIEW
--
-- Account Lifecycle:
-- [Account Created]
--        ↓
-- [Set statement_cycle_day and next_billing_date]
--
-- Daily Scheduled Processing:
-- ────────────────────────────────────────────────────────────
-- 1. Scheduler runs daily
-- 2. Fetch credit accounts where next_billing_date = today
-- 3. Generate billing statements for due accounts
-- 4. Persist statements and update account details
--    (including recalculating next_billing_date)
--
--
-- Notes:
-- - next_billing_date is a key field driving billing execution
-- - Indexing improves performance for daily scheduler queries
--
-- ============================================================

-- Add a column to store the next billing date for each credit account
ALTER TABLE credit_accounts
ADD COLUMN next_billing_date DATE;

-- Create an index to optimize lookups for accounts due for billing
CREATE INDEX idx_next_billing_date
ON credit_accounts (next_billing_date);