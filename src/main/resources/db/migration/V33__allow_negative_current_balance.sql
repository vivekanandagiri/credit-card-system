----------------------------
--Allow Negetive balance ---------
--Remove CONSTRAINT
ALTER TABLE credit_accounts
DROP CONSTRAINT IF EXISTS chk_current_balance;

ALTER TABLE credit_accounts
ADD CONSTRAINT chk_current_balance
CHECK (
    current_balance >= -1000000000
);