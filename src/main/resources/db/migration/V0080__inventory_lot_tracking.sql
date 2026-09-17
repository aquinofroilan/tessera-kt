-- Add lot tracking flag to products
ALTER TABLE products ADD COLUMN is_lot_tracked BOOLEAN NOT NULL DEFAULT FALSE;

-- Add lot_number to stock_movements
ALTER TABLE stock_movements ADD COLUMN lot_number VARCHAR(128);

-- Add lot_number to stock_on_hand and update unique constraint
ALTER TABLE stock_on_hand ADD COLUMN lot_number VARCHAR(128) NOT NULL DEFAULT '';
ALTER TABLE stock_on_hand DROP CONSTRAINT stock_on_hand_organization_id_product_id_warehouse_id_key;
ALTER TABLE stock_on_hand ADD CONSTRAINT stock_on_hand_org_prod_wh_lot_key UNIQUE (organization_id, product_id, warehouse_id, lot_number);
