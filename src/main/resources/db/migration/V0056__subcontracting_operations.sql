-- V0056__subcontracting_operations.sql
-- Subcontracting and outsourced manufacturing operations tracking

CREATE TABLE IF NOT EXISTS mfg_subcontract_orders (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    order_number VARCHAR(64) NOT NULL,
    work_order_id UUID NOT NULL REFERENCES mfg_work_orders(id) ON DELETE CASCADE,
    operation_id UUID,
    operation_number INT NOT NULL,
    vendor_id UUID NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    purchase_order_id UUID REFERENCES purchase_orders(id) ON DELETE SET NULL,
    service_item_name VARCHAR(255) NOT NULL,
    quantity NUMERIC(18, 4) NOT NULL,
    received_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0,
    unit_service_cost NUMERIC(18, 4) NOT NULL DEFAULT 0,
    total_cost NUMERIC(18, 4) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    dispatched_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    notes TEXT,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_subcontract_order_number UNIQUE (organization_id, order_number)
);

CREATE TABLE IF NOT EXISTS mfg_subcontract_components (
    id UUID PRIMARY KEY,
    subcontract_order_id UUID NOT NULL REFERENCES mfg_subcontract_orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_sku VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    planned_quantity NUMERIC(18, 4) NOT NULL,
    dispatched_quantity NUMERIC(18, 4) NOT NULL DEFAULT 0,
    uom VARCHAR(32)
);

CREATE INDEX IF NOT EXISTS idx_subcontract_orders_org ON mfg_subcontract_orders(organization_id);
CREATE INDEX IF NOT EXISTS idx_subcontract_orders_wo ON mfg_subcontract_orders(organization_id, work_order_id);
CREATE INDEX IF NOT EXISTS idx_subcontract_orders_vendor ON mfg_subcontract_orders(organization_id, vendor_id);
CREATE INDEX IF NOT EXISTS idx_subcontract_orders_status ON mfg_subcontract_orders(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_subcontract_components_order ON mfg_subcontract_components(subcontract_order_id);
