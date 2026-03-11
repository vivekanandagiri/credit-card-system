-- =====================================================
-- CUSTOMER RESIDENCY
-- =====================================================
ALTER TABLE customers
ADD COLUMN residency_status VARCHAR(100) NOT NULL DEFAULT 'UNKNOWN',
ADD COLUMN citizenship_country VARCHAR(100) NOT NULL DEFAULT 'UNKNOWN';


-- =====================================================
-- CUSTOMER ADDRESSES
-- =====================================================
CREATE TABLE customer_addresses (
    address_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL,

    line1 VARCHAR(255) NOT NULL,
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL,

    CONSTRAINT fk_address_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id)
        ON DELETE CASCADE
);