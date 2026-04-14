-- Enforce that every credit account must have a next_billing_date
-- This ensures the billing scheduler can reliably identify due accounts
-- 
ALTER TABLE credit_accounts
ALTER COLUMN next_billing_date SET NOT NULL;

-- Ensure billing date is not invalid
ALTER TABLE credit_accounts
ADD CONSTRAINT chk_next_billing_date_future
CHECK (next_billing_date >= CURRENT_DATE);
