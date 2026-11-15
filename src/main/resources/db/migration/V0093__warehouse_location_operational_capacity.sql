ALTER TABLE warehouse_locations
    ADD COLUMN is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN is_pickable BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN is_receivable BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN max_weight_kg NUMERIC(18, 6),
    ADD COLUMN max_volume_cbm NUMERIC(18, 6);

ALTER TABLE products
    ADD COLUMN unit_weight_kg NUMERIC(18, 6),
    ADD COLUMN unit_volume_cbm NUMERIC(18, 6);
