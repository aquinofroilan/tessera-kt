-- Inventory: physical count & reconciliation.
-- A count session snapshots the on-hand quantity for every product at a
-- chosen warehouse at creation time. Counters then record the actual
-- physical count per line; at post time the variance for each line is
-- reconciled into an ADJUSTMENT stock movement so the GL stays in sync.

CREATE TABLE inventory_count_sessions (
    id                uuid PRIMARY KEY,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    code              text NOT NULL,
    warehouse_id      uuid NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    status            text NOT NULL DEFAULT 'DRAFT',
    scheduled_for     date,
    started_at        timestamp,
    posted_at         timestamp,
    posted_by         uuid REFERENCES users(uuid),
    notes             text,
    created_by        uuid NOT NULL REFERENCES users(uuid),
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT inv_count_status_chk CHECK (status IN ('DRAFT','COUNTING','POSTED','CANCELLED')),
    CONSTRAINT inv_count_unique_code_per_org UNIQUE (organization_id, code)
);
CREATE INDEX inv_count_org_warehouse_idx ON inventory_count_sessions(organization_id, warehouse_id);
CREATE INDEX inv_count_org_status_idx    ON inventory_count_sessions(organization_id, status);

CREATE TABLE inventory_count_lines (
    id                       uuid PRIMARY KEY,
    session_id               uuid NOT NULL REFERENCES inventory_count_sessions(id) ON DELETE CASCADE,
    line_number              int NOT NULL,
    product_id               uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_sku              text NOT NULL,
    product_name             text NOT NULL,
    expected_quantity        numeric(18,4) NOT NULL,
    counted_quantity         numeric(18,4),
    variance_quantity        numeric(18,4),
    notes                    text,
    adjustment_movement_id   uuid,
    CONSTRAINT inv_count_lines_unique_per_session UNIQUE (session_id, line_number)
);
CREATE INDEX inv_count_lines_product_idx ON inventory_count_lines(product_id);
