-- Manufacturing: standard cost rollup.
-- A single-level rollup of a product's standard cost: sum of (component
-- standard cost x BOM quantity, including scrap) plus labour cost
-- (work-center cost_per_hour x routing minutes). The result is persisted
-- per product so downstream COGS and variance reporting can reference a
-- stable cost baseline that doesn't shift with every issue/receipt.
--
-- Single-level only: component standard costs are taken from this table
-- (cascading rollups for sub-assemblies are out of scope for this slice;
-- callers can rebuild bottom-up by rolling up sub-assemblies first).

CREATE TABLE mfg_product_standard_costs (
    id                uuid PRIMARY KEY,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    product_id        uuid NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    bom_id            uuid REFERENCES mfg_boms(id) ON DELETE SET NULL,
    routing_id        uuid REFERENCES mfg_routings(id) ON DELETE SET NULL,
    material_cost     numeric(18,4) NOT NULL DEFAULT 0,
    labor_cost        numeric(18,4) NOT NULL DEFAULT 0,
    overhead_cost     numeric(18,4) NOT NULL DEFAULT 0,
    total_cost        numeric(18,4) NOT NULL DEFAULT 0,
    source            text NOT NULL DEFAULT 'rollup',
    calculated_at     timestamp NOT NULL DEFAULT current_timestamp,
    calculated_by     uuid NOT NULL REFERENCES users(uuid),
    notes             text,
    CONSTRAINT mfg_psc_source_chk CHECK (source IN ('rollup','manual')),
    CONSTRAINT mfg_psc_unique_per_product UNIQUE (organization_id, product_id)
);
CREATE INDEX mfg_psc_product_idx ON mfg_product_standard_costs(product_id);
