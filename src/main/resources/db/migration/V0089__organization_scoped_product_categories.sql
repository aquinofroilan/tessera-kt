CREATE TABLE product_categories (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid) ON DELETE CASCADE,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(2000),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT product_categories_org_name_unique UNIQUE (organization_id, name)
);

CREATE INDEX product_categories_org_active_idx ON product_categories(organization_id, is_active);

ALTER TABLE products DROP COLUMN category;
ALTER TABLE products ADD COLUMN category_id UUID REFERENCES product_categories(id) ON DELETE RESTRICT;
CREATE INDEX products_org_category_idx ON products(organization_id, category_id);
