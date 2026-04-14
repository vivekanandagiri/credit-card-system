-- 1. Add payment_id column
ALTER TABLE transactions
ADD COLUMN payment_id UUID;

-- 2. Add foreign key constraint
ALTER TABLE transactions
ADD CONSTRAINT fk_transactions_payment
FOREIGN KEY (payment_id)
REFERENCES payments(payment_id)
ON DELETE SET NULL;

-- 3. (Optional but recommended) Add index for performance
CREATE INDEX idx_transactions_payment_id
ON transactions(payment_id);