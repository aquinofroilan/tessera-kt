ALTER TABLE products ADD COLUMN has_expiry BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE product_lots (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    product_id UUID NOT NULL REFERENCES products(id),
    lot_number VARCHAR(100) NOT NULL,
    expiry_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_product_lots_org_product_lot UNIQUE (organization_id, product_id, lot_number)
);
