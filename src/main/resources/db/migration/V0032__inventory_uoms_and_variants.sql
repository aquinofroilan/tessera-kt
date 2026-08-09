-- Inventory: units of measure (UoMs) and product variants.
-- A UoM defines how product quantity is expressed (EACH, KG, BOX-of-12).
-- Non-base UoMs convert to a base UoM via conversion_factor so reports can
-- normalise. Variants let a single product carry per-SKU attributes
-- (size/color/configuration) without forcing a separate product per variant.

CREATE TABLE inventory_uoms (
    id                 uuid PRIMARY KEY,
    organization_id    uuid NOT NULL REFERENCES organizations(uuid),
    code               text NOT NULL,
    name               text NOT NULL,
    description        text,
    base_uom_id        uuid REFERENCES inventory_uoms(id) ON DELETE RESTRICT,
    conversion_factor  numeric(18,6) NOT NULL DEFAULT 1,
    is_active          boolean NOT NULL DEFAULT true,
    created_at         timestamp NOT NULL DEFAULT current_timestamp,
    updated_at         timestamp,
    CONSTRAINT inv_uoms_conv_positive_chk CHECK (conversion_factor > 0),
    CONSTRAINT inv_uoms_unique_code_per_org UNIQUE (organization_id, code)
);
CREATE INDEX inv_uoms_org_active_idx ON inventory_uoms(organization_id, is_active);

CREATE TABLE inventory_product_variants (
    id              uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(uuid),
    product_id      uuid NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    code            text NOT NULL,
    name            text NOT NULL,
    sku_suffix      text,
    attributes      jsonb NOT NULL DEFAULT '{}'::jsonb,
    is_active       boolean NOT NULL DEFAULT true,
    created_at      timestamp NOT NULL DEFAULT current_timestamp,
    updated_at      timestamp,
    CONSTRAINT inv_product_variants_unique_code UNIQUE (product_id, code)
);
CREATE INDEX inv_product_variants_org_product_idx ON inventory_product_variants(organization_id, product_id);

ALTER TABLE products
    ADD COLUMN uom_id uuid REFERENCES inventory_uoms(id) ON DELETE SET NULL;
CREATE INDEX products_uom_idx ON products(uom_id);
