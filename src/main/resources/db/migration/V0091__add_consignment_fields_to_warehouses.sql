ALTER TABLE warehouses
    ADD COLUMN is_consignment boolean NOT NULL DEFAULT false,
    ADD COLUMN customer_id uuid REFERENCES customers(id) ON DELETE SET NULL;
