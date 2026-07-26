-- Manufacturing: Bill of Materials (BOM).
-- A BOM describes the component products required to produce one unit of a
-- parent product. BOMs are versioned and have an effective-date window; at most
-- one BOM may be flagged is_default = true for a given (organization, product)
-- and only ACTIVE BOMs are usable on work orders (added in a later migration).

CREATE TABLE mfg_boms (
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
    CONSTRAINT mfg_boms_status_chk CHECK (status IN ('DRAFT','ACTIVE','OBSOLETE')),
    CONSTRAINT mfg_boms_version_chk CHECK (version > 0),
    CONSTRAINT mfg_boms_dates_chk CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from),
    CONSTRAINT mfg_boms_unique_code_per_org UNIQUE (organization_id, code)
);
CREATE INDEX mfg_boms_org_product_idx ON mfg_boms(organization_id, product_id);
CREATE INDEX mfg_boms_org_status_idx  ON mfg_boms(organization_id, status);
CREATE UNIQUE INDEX mfg_boms_one_default_per_product
    ON mfg_boms(organization_id, product_id)
    WHERE is_default = true;

CREATE TABLE mfg_bom_lines (
    id                     uuid PRIMARY KEY,
    bom_id                 uuid NOT NULL REFERENCES mfg_boms(id) ON DELETE CASCADE,
    line_number            int  NOT NULL,
    component_product_id   uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    component_sku          text NOT NULL,
    component_name         text NOT NULL,
    quantity               numeric(18,4) NOT NULL,
    uom                    text,
    scrap_pct              numeric(7,4) NOT NULL DEFAULT 0,
    notes                  text,
    CONSTRAINT mfg_bom_lines_qty_chk   CHECK (quantity > 0),
    CONSTRAINT mfg_bom_lines_scrap_chk CHECK (scrap_pct >= 0 AND scrap_pct < 100),
    CONSTRAINT mfg_bom_lines_unique_line_per_bom UNIQUE (bom_id, line_number),
    CONSTRAINT mfg_bom_lines_unique_component_per_bom UNIQUE (bom_id, component_product_id)
);
CREATE INDEX mfg_bom_lines_component_idx ON mfg_bom_lines(component_product_id);
