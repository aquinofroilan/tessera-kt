-- Manufacturing: work orders.
-- A work order produces a quantity of a finished product by consuming the
-- components declared on a BOM and executing the operations declared on a
-- routing. At release time the BOM and (optional) routing snapshots are
-- copied to wo_components / wo_operations so subsequent revisions to the
-- master data do not retroactively alter the order. Material consumption,
-- completion, and GL posting are handled in the next migration / service.

CREATE TABLE mfg_work_orders (
    id                     uuid PRIMARY KEY,
    organization_id        uuid NOT NULL REFERENCES organizations(uuid),
    wo_number              text NOT NULL,
    product_id             uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_sku            text NOT NULL,
    product_name           text NOT NULL,
    bom_id                 uuid NOT NULL REFERENCES mfg_boms(id) ON DELETE RESTRICT,
    routing_id             uuid REFERENCES mfg_routings(id) ON DELETE RESTRICT,
    quantity               numeric(18,4) NOT NULL,
    quantity_completed     numeric(18,4) NOT NULL DEFAULT 0,
    quantity_scrapped      numeric(18,4) NOT NULL DEFAULT 0,
    source_warehouse_id    uuid NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    target_warehouse_id    uuid NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    status                 text NOT NULL DEFAULT 'DRAFT',
    planned_start          date,
    planned_end            date,
    released_at            timestamp,
    released_by            uuid REFERENCES users(uuid),
    started_at             timestamp,
    completed_at           timestamp,
    completed_by           uuid REFERENCES users(uuid),
    cancelled_at           timestamp,
    cancelled_by           uuid REFERENCES users(uuid),
    notes                  text,
    created_by             uuid NOT NULL REFERENCES users(uuid),
    created_at             timestamp NOT NULL DEFAULT current_timestamp,
    updated_at             timestamp,
    CONSTRAINT mfg_wo_status_chk CHECK (status IN ('DRAFT','RELEASED','IN_PROGRESS','COMPLETED','CLOSED','CANCELLED')),
    CONSTRAINT mfg_wo_qty_chk    CHECK (quantity > 0),
    CONSTRAINT mfg_wo_qty_progress_chk
        CHECK (quantity_completed >= 0 AND quantity_scrapped >= 0
               AND (quantity_completed + quantity_scrapped) <= quantity),
    CONSTRAINT mfg_wo_dates_chk  CHECK (planned_end IS NULL OR planned_start IS NULL OR planned_end >= planned_start),
    CONSTRAINT mfg_wo_unique_number_per_org UNIQUE (organization_id, wo_number)
);
CREATE INDEX mfg_wo_org_status_idx  ON mfg_work_orders(organization_id, status);
CREATE INDEX mfg_wo_org_product_idx ON mfg_work_orders(organization_id, product_id);

CREATE TABLE mfg_wo_components (
    id                     uuid PRIMARY KEY,
    work_order_id          uuid NOT NULL REFERENCES mfg_work_orders(id) ON DELETE CASCADE,
    line_number            int  NOT NULL,
    component_product_id   uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    component_sku          text NOT NULL,
    component_name         text NOT NULL,
    planned_quantity       numeric(18,4) NOT NULL,
    issued_quantity        numeric(18,4) NOT NULL DEFAULT 0,
    uom                    text,
    scrap_pct              numeric(7,4) NOT NULL DEFAULT 0,
    CONSTRAINT mfg_woc_qty_chk        CHECK (planned_quantity > 0),
    CONSTRAINT mfg_woc_issued_chk     CHECK (issued_quantity >= 0),
    CONSTRAINT mfg_woc_unique_per_wo  UNIQUE (work_order_id, line_number)
);
CREATE INDEX mfg_woc_component_idx ON mfg_wo_components(component_product_id);

CREATE TABLE mfg_wo_operations (
    id                     uuid PRIMARY KEY,
    work_order_id          uuid NOT NULL REFERENCES mfg_work_orders(id) ON DELETE CASCADE,
    operation_number       int  NOT NULL,
    work_center_id         uuid NOT NULL REFERENCES mfg_work_centers(id) ON DELETE RESTRICT,
    work_center_code       text NOT NULL,
    description            text NOT NULL,
    planned_setup_minutes  numeric(18,4) NOT NULL DEFAULT 0,
    planned_run_minutes_per_unit numeric(18,4) NOT NULL DEFAULT 0,
    actual_minutes         numeric(18,4) NOT NULL DEFAULT 0,
    status                 text NOT NULL DEFAULT 'PENDING',
    CONSTRAINT mfg_woop_status_chk     CHECK (status IN ('PENDING','IN_PROGRESS','DONE','SKIPPED')),
    CONSTRAINT mfg_woop_unique_per_wo  UNIQUE (work_order_id, operation_number)
);
CREATE INDEX mfg_woop_wc_idx ON mfg_wo_operations(work_center_id);
