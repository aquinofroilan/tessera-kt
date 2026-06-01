-- Procurement: purchase requests (requisitions).
-- A purchase request is the requisition stage that precedes a purchase order:
-- draft -> submit -> approve/reject, and an approved request can be converted
-- into a purchase order (linked via converted_purchase_order_id).
CREATE TABLE purchase_requests (
    id                          uuid PRIMARY KEY,
    pr_number                   text NOT NULL,
    organization_id             uuid NOT NULL REFERENCES organizations(uuid),
    status                      text NOT NULL DEFAULT 'DRAFT',
    suggested_vendor_id         uuid REFERENCES vendors(id) ON DELETE SET NULL,
    warehouse_id                uuid REFERENCES warehouses(id) ON DELETE SET NULL,
    justification               text,
    requested_by                uuid NOT NULL REFERENCES users(uuid),
    decided_by                  uuid REFERENCES users(uuid),
    decided_at                  timestamp,
    decision_reason             text,
    converted_purchase_order_id uuid REFERENCES purchase_orders(id) ON DELETE SET NULL,
    created_at                  timestamp NOT NULL DEFAULT current_timestamp,
    updated_at                  timestamp,
    CONSTRAINT purchase_requests_status_chk
        CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','REJECTED','CONVERTED','CANCELLED')),
    CONSTRAINT purchase_requests_unique_number_per_org UNIQUE (organization_id, pr_number)
);
CREATE INDEX purchase_requests_org_status_idx ON purchase_requests(organization_id, status);

CREATE TABLE purchase_request_lines (
    id                   uuid PRIMARY KEY,
    purchase_request_id  uuid NOT NULL REFERENCES purchase_requests(id) ON DELETE CASCADE,
    line_number          int NOT NULL,
    product_id           uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_sku          text NOT NULL,
    product_name         text NOT NULL,
    quantity             numeric(18,4) NOT NULL,
    estimated_unit_cost  numeric(18,4),
    description          text,
    CONSTRAINT purchase_request_lines_unique_line_per_pr UNIQUE (purchase_request_id, line_number)
);
CREATE INDEX purchase_request_lines_product_idx ON purchase_request_lines(product_id);
