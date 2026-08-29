CREATE TABLE mfg_engineering_change_orders (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    eco_number VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    effective_date DATE,
    requested_by UUID NOT NULL,
    approved_by UUID,
    approved_at TIMESTAMP,
    implemented_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE mfg_eco_affected_items (
    id UUID PRIMARY KEY,
    eco_id UUID NOT NULL REFERENCES mfg_engineering_change_orders(id) ON DELETE CASCADE,
    item_type VARCHAR(50) NOT NULL,
    old_version_id UUID,
    new_version_id UUID NOT NULL,
    notes TEXT
);

CREATE UNIQUE INDEX idx_mfg_eco_number_org ON mfg_engineering_change_orders(organization_id, eco_number);
