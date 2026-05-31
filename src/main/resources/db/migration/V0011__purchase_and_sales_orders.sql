-- Purchasing and Sales order flows.
-- Purchase orders drive goods receipts (stock RECEIPT movements); sales orders
-- drive fulfillment (stock ISSUE movements). Bill/invoice generation is a
-- follow-up phase.

CREATE TABLE purchase_orders (
    id                uuid PRIMARY KEY,
    po_number         text NOT NULL,
    vendor_id         uuid NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    vendor_name       text NOT NULL,
    warehouse_id      uuid NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    order_date        date NOT NULL,
    expected_date     date,
    reference_number  text,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    status            text NOT NULL DEFAULT 'DRAFT',
    total_amount      numeric(18,4) NOT NULL,
    created_by        uuid NOT NULL REFERENCES users(uuid),
    approved_at       timestamp,
    approved_by       uuid REFERENCES users(uuid),
    received_at       timestamp,
    cancelled_at      timestamp,
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT purchase_orders_status_chk CHECK (status IN ('DRAFT','APPROVED','RECEIVED','CLOSED','CANCELLED')),
    CONSTRAINT purchase_orders_unique_number_per_org UNIQUE (organization_id, po_number)
);
CREATE INDEX purchase_orders_org_status_idx ON purchase_orders(organization_id, status);
CREATE INDEX purchase_orders_org_vendor_idx ON purchase_orders(organization_id, vendor_id);

CREATE TABLE purchase_order_lines (
    id                 uuid PRIMARY KEY,
    purchase_order_id  uuid NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    line_number        int NOT NULL,
    product_id         uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_sku        text NOT NULL,
    product_name       text NOT NULL,
    quantity           numeric(18,4) NOT NULL,
    unit_cost          numeric(18,4) NOT NULL,
    line_total         numeric(18,4) NOT NULL,
    description        text,
    CONSTRAINT purchase_order_lines_unique_line_per_po UNIQUE (purchase_order_id, line_number)
);
CREATE INDEX purchase_order_lines_product_idx ON purchase_order_lines(product_id);

CREATE TABLE sales_orders (
    id                uuid PRIMARY KEY,
    so_number         text NOT NULL,
    customer_id       uuid NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    customer_name     text NOT NULL,
    warehouse_id      uuid NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    order_date        date NOT NULL,
    expected_date     date,
    reference_number  text,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    status            text NOT NULL DEFAULT 'DRAFT',
    total_amount      numeric(18,4) NOT NULL,
    created_by        uuid NOT NULL REFERENCES users(uuid),
    approved_at       timestamp,
    approved_by       uuid REFERENCES users(uuid),
    fulfilled_at      timestamp,
    cancelled_at      timestamp,
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT sales_orders_status_chk CHECK (status IN ('DRAFT','APPROVED','FULFILLED','CLOSED','CANCELLED')),
    CONSTRAINT sales_orders_unique_number_per_org UNIQUE (organization_id, so_number)
);
CREATE INDEX sales_orders_org_status_idx ON sales_orders(organization_id, status);
CREATE INDEX sales_orders_org_customer_idx ON sales_orders(organization_id, customer_id);

CREATE TABLE sales_order_lines (
    id              uuid PRIMARY KEY,
    sales_order_id  uuid NOT NULL REFERENCES sales_orders(id) ON DELETE CASCADE,
    line_number     int NOT NULL,
    product_id      uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_sku     text NOT NULL,
    product_name    text NOT NULL,
    quantity        numeric(18,4) NOT NULL,
    unit_price      numeric(18,4) NOT NULL,
    line_total      numeric(18,4) NOT NULL,
    description     text,
    CONSTRAINT sales_order_lines_unique_line_per_so UNIQUE (sales_order_id, line_number)
);
CREATE INDEX sales_order_lines_product_idx ON sales_order_lines(product_id);
