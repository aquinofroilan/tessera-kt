-- Add held_quantity to stock_on_hand
ALTER TABLE stock_on_hand ADD COLUMN held_quantity NUMERIC(18,4) NOT NULL DEFAULT 0;

-- Create inventory_holds for auditability
CREATE TABLE inventory_holds (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    product_id UUID NOT NULL REFERENCES products(id),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    lot_number VARCHAR(128),
    serial_numbers JSONB,
    quantity NUMERIC(18,4) NOT NULL,
    reference VARCHAR(255),
    notes TEXT,
    status VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT current_timestamp,
    created_by UUID NOT NULL,
    released_at TIMESTAMP,
    released_by UUID
);
