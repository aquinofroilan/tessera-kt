-- Manufacturing: production resources (work centers) and routings.
-- A work center is a labor or machine resource that consumes time during a
-- production run; a routing is the ordered sequence of operations needed to
-- transform components into a finished product. Routings reference the
-- BOM-produced product and -- like BOMs -- are versioned with a draft/active/
-- obsolete lifecycle and an optional default flag per product.

CREATE TABLE mfg_work_centers (
    id                uuid PRIMARY KEY,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    code              text NOT NULL,
    name              text NOT NULL,
    description       text,
    type              text NOT NULL DEFAULT 'MACHINE',
    warehouse_id      uuid REFERENCES warehouses(id) ON DELETE SET NULL,
    capacity_per_hour numeric(18,4) NOT NULL DEFAULT 1,
    cost_per_hour     numeric(18,4) NOT NULL DEFAULT 0,
    efficiency_pct    numeric(7,4) NOT NULL DEFAULT 100,
    is_active         boolean NOT NULL DEFAULT true,
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT mfg_wc_type_chk     CHECK (type IN ('MACHINE','LABOR','CELL','ASSEMBLY','SUBCONTRACT')),
    CONSTRAINT mfg_wc_capacity_chk CHECK (capacity_per_hour > 0),
    CONSTRAINT mfg_wc_cost_chk     CHECK (cost_per_hour >= 0),
    CONSTRAINT mfg_wc_eff_chk      CHECK (efficiency_pct > 0 AND efficiency_pct <= 200),
    CONSTRAINT mfg_wc_unique_code_per_org UNIQUE (organization_id, code)
);
CREATE INDEX mfg_wc_org_active_idx ON mfg_work_centers(organization_id, is_active);

CREATE TABLE mfg_routings (
    id                uuid PRIMARY KEY,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    product_id        uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    code              text NOT NULL,
    name              text NOT NULL,
    version           int  NOT NULL DEFAULT 1,
    status            text NOT NULL DEFAULT 'DRAFT',
    is_default        boolean NOT NULL DEFAULT false,
    effective_from    date,
    effective_to      date,
    notes             text,
    created_by        uuid NOT NULL REFERENCES users(uuid),
    activated_at      timestamp,
    activated_by      uuid REFERENCES users(uuid),
    obsoleted_at      timestamp,
    obsoleted_by      uuid REFERENCES users(uuid),
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT mfg_routings_status_chk  CHECK (status IN ('DRAFT','ACTIVE','OBSOLETE')),
    CONSTRAINT mfg_routings_version_chk CHECK (version > 0),
    CONSTRAINT mfg_routings_dates_chk
        CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from),
    CONSTRAINT mfg_routings_unique_code_per_org UNIQUE (organization_id, code)
);
CREATE INDEX mfg_routings_org_product_idx ON mfg_routings(organization_id, product_id);
CREATE INDEX mfg_routings_org_status_idx  ON mfg_routings(organization_id, status);
CREATE UNIQUE INDEX mfg_routings_one_default_per_product
    ON mfg_routings(organization_id, product_id)
    WHERE is_default = true;

CREATE TABLE mfg_routing_operations (
    id                       uuid PRIMARY KEY,
    routing_id               uuid NOT NULL REFERENCES mfg_routings(id) ON DELETE CASCADE,
    operation_number         int  NOT NULL,
    work_center_id           uuid NOT NULL REFERENCES mfg_work_centers(id) ON DELETE RESTRICT,
    work_center_code         text NOT NULL,
    description              text NOT NULL,
    setup_minutes            numeric(18,4) NOT NULL DEFAULT 0,
    run_minutes_per_unit     numeric(18,4) NOT NULL DEFAULT 0,
    queue_minutes            numeric(18,4) NOT NULL DEFAULT 0,
    instructions             text,
    CONSTRAINT mfg_routing_ops_setup_chk CHECK (setup_minutes   >= 0),
    CONSTRAINT mfg_routing_ops_run_chk   CHECK (run_minutes_per_unit >= 0),
    CONSTRAINT mfg_routing_ops_queue_chk CHECK (queue_minutes   >= 0),
    CONSTRAINT mfg_routing_ops_unique_op_per_routing UNIQUE (routing_id, operation_number)
);
CREATE INDEX mfg_routing_ops_wc_idx ON mfg_routing_operations(work_center_id);
