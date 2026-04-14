ALTER TABLE credit_accounts
DROP COLUMN IF EXISTS next_billing_date;

ALTER TABLE credit_accounts
DROP COLUMN IF EXISTS minimum_due_amount;