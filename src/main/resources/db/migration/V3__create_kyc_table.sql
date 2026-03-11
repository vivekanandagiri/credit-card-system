
-- KYC STATUS ENUM

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type WHERE typname = 'kyc_status'
    ) THEN
        CREATE TYPE kyc_status AS ENUM (
            'SUBMITTED',
            'VERIFIED',
            'REJECTED',
            'RESUBMIT_REQUIRED'
        );
    END IF;
END$$;


-- KYC RECORDS
CREATE TABLE kyc_records (
    kyc_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL,

    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100) NOT NULL,

    document_file BYTEA NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,

    status kyc_status NOT NULL,
    rejection_reason VARCHAR(500),

    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMPTZ,
    verified_by UUID,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL,

    CONSTRAINT fk_kyc_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_kyc_customer ON kyc_records(customer_id);
CREATE INDEX idx_kyc_status ON kyc_records(status);
CREATE INDEX idx_kyc_active ON kyc_records(is_active);

CREATE UNIQUE INDEX unique_active_kyc_per_customer
ON kyc_records(customer_id)
WHERE is_active = TRUE;