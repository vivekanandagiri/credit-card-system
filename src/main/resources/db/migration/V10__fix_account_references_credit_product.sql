-- =====================================================
-- Changes on credit_accounts:
--   DROP   card_product_id   (FK to credit_card_products)
--   ADD    credit_product_id (FK to credit_products)
-- =====================================================

ALTER TABLE credit_accounts
    DROP CONSTRAINT IF EXISTS fk_account_card_product;
 
DROP INDEX IF EXISTS idx_account_card_product;
 
ALTER TABLE credit_accounts
    DROP COLUMN IF EXISTS card_product_id;
 
ALTER TABLE credit_accounts
    ADD COLUMN credit_product_id BIGINT NOT NULL;
 
ALTER TABLE credit_accounts
    ADD CONSTRAINT fk_account_credit_product
        FOREIGN KEY (credit_product_id)
        REFERENCES credit_products(credit_product_id)
        ON DELETE RESTRICT;
 
CREATE INDEX IF NOT EXISTS idx_account_credit_product
    ON credit_accounts(credit_product_id);