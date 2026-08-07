-- Manufacturing: Master Production Schedule (MPS).
-- A list of demand entries -- product, quantity, target date -- per
-- organisation. MRP and CRP are read-only computations over this table
-- plus stock-on-hand and the BOM/routing graphs; we don't persist the
-- computed plan, only the demand it operates on, so re-running with a
-- different cutoff is cheap.

CREATE TABLE mfg_mps_entries (
    id                uuid PRIMARY KEY,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    product_id        uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_sku       text NOT NULL,
    product_name      text NOT NULL,
    quantity          numeric(18,4) NOT NULL,
    required_by       date NOT NULL,
    status            text NOT NULL DEFAULT 'PLANNED',
    notes             text,
    created_by        uuid NOT NULL REFERENCES users(uuid),
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT mfg_mps_qty_chk    CHECK (quantity > 0),
    CONSTRAINT mfg_mps_status_chk CHECK (status IN ('PLANNED','FIRM','RELEASED','CANCELLED'))
);
CREATE INDEX mfg_mps_org_date_idx    ON mfg_mps_entries(organization_id, required_by);
CREATE INDEX mfg_mps_org_product_idx ON mfg_mps_entries(organization_id, product_id);
