
-- Convert DATE → TIMESTAMPTZ

ALTER TABLE credit_accounts
    ALTER COLUMN last_payment_date TYPE TIMESTAMPTZ
    USING last_payment_date::timestamp;

ALTER TABLE credit_accounts
    ALTER COLUMN last_statement_date TYPE TIMESTAMPTZ
    USING last_statement_date::timestamp;

ALTER TABLE credit_accounts
    ALTER COLUMN next_due_date TYPE TIMESTAMPTZ
    USING next_due_date::timestamp;