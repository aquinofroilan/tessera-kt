CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    gateway VARCHAR(50) NOT NULL,
    gateway_intent_id VARCHAR(255) NOT NULL UNIQUE,
    gross_amount NUMERIC(19, 4) NOT NULL,
    fee_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0,
    net_amount NUMERIC(19, 4) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_transactions_intent ON payment_transactions(gateway_intent_id);
