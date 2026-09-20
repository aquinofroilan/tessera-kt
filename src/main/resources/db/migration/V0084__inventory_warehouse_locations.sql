-- Create warehouse locations table
CREATE TABLE warehouse_locations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    parent_location_id UUID REFERENCES warehouse_locations(id),
    code VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    barcode VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMP NOT NULL DEFAULT current_timestamp,
    UNIQUE (organization_id, warehouse_id, code)
);

-- Update stock_on_hand
ALTER TABLE stock_on_hand ADD COLUMN location_id UUID REFERENCES warehouse_locations(id);

ALTER TABLE stock_on_hand DROP CONSTRAINT stock_on_hand_org_prod_wh_lot_key;

CREATE UNIQUE INDEX stock_on_hand_org_prod_wh_loc_lot_key 
    ON stock_on_hand (organization_id, product_id, warehouse_id, COALESCE(location_id, '00000000-0000-0000-0000-000000000000'::uuid), lot_number);

-- Update stock_movements
ALTER TABLE stock_movements ADD COLUMN source_location_id UUID REFERENCES warehouse_locations(id);
ALTER TABLE stock_movements ADD COLUMN destination_location_id UUID REFERENCES warehouse_locations(id);

-- Update product_serials
ALTER TABLE product_serials ADD COLUMN current_location_id UUID REFERENCES warehouse_locations(id);
