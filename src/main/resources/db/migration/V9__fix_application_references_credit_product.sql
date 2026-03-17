-- =====================================================
--   DROP   card_product_id  (FK to credit_card_products)
--   ADD    credit_product_id (FK to credit_products)
-- =====================================================

TRUNCATE TABLE credit_accounts CASCADE;
TRUNCATE TABLE credit_card_applications CASCADE;
 
ALTER TABLE credit_card_applications
    DROP CONSTRAINT IF EXISTS fk_application_card_product;
 
DROP INDEX IF EXISTS idx_application_card_product;
 
ALTER TABLE credit_card_applications
    DROP COLUMN IF EXISTS card_product_id;
 
ALTER TABLE credit_card_applications
    ADD COLUMN credit_product_id BIGINT NOT NULL;
 
ALTER TABLE credit_card_applications
    ADD CONSTRAINT fk_application_credit_product
        FOREIGN KEY (credit_product_id)
        REFERENCES credit_products(credit_product_id)
        ON DELETE RESTRICT;
 
CREATE INDEX IF NOT EXISTS idx_application_credit_product
    ON credit_card_applications(credit_product_id);