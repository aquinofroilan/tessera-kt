ALTER TABLE products ADD COLUMN is_serialized BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE product_serials (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    product_id UUID NOT NULL REFERENCES products(id),
    serial_number VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    current_warehouse_id UUID REFERENCES warehouses(id),
    lot_number VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_product_serials_org_product_serial UNIQUE (organization_id, product_id, serial_number)
);

ALTER TABLE stock_movements ADD COLUMN serial_numbers JSONB;
