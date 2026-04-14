-- Allow transactions without a card (UPI, NET_BANKING, etc.)

ALTER TABLE transactions
ALTER COLUMN card_id DROP NOT NULL;