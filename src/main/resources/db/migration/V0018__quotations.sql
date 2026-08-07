-- Sales: quotations.
-- A quotation is the quote stage that precedes a sales order: draft -> send ->
-- accept/reject, and an accepted quote can be converted into a sales order
-- (linked via converted_sales_order_id).
CREATE TABLE quotations (
    id                       uuid PRIMARY KEY,
    quote_number             text NOT NULL,
    customer_id              uuid NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    customer_name            text NOT NULL,
    warehouse_id             uuid REFERENCES warehouses(id) ON DELETE SET NULL,
    quote_date               date NOT NULL,
    valid_until              date,
    reference_number         text,
    organization_id          uuid NOT NULL REFERENCES organizations(uuid),
    status                   text NOT NULL DEFAULT 'DRAFT',
    total_amount             numeric(18,4) NOT NULL,
    created_by               uuid NOT NULL REFERENCES users(uuid),
    sent_at                  timestamp,
    decided_at               timestamp,
    decision_reason          text,
    converted_sales_order_id uuid REFERENCES sales_orders(id) ON DELETE SET NULL,
    created_at               timestamp NOT NULL DEFAULT current_timestamp,
    updated_at               timestamp,
    CONSTRAINT quotations_status_chk
        CHECK (status IN ('DRAFT','SENT','ACCEPTED','REJECTED','CONVERTED','CANCELLED')),
    CONSTRAINT quotations_unique_number_per_org UNIQUE (organization_id, quote_number)
);
CREATE INDEX quotations_org_status_idx ON quotations(organization_id, status);
CREATE INDEX quotations_org_customer_idx ON quotations(organization_id, customer_id);

CREATE TABLE quotation_lines (
    id              uuid PRIMARY KEY,
    quotation_id    uuid NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
    line_number     int NOT NULL,
    product_id      uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_sku     text NOT NULL,
    product_name    text NOT NULL,
    quantity        numeric(18,4) NOT NULL,
    unit_price      numeric(18,4) NOT NULL,
    line_total      numeric(18,4) NOT NULL,
    description     text,
    CONSTRAINT quotation_lines_unique_line_per_quote UNIQUE (quotation_id, line_number)
);
CREATE INDEX quotation_lines_product_idx ON quotation_lines(product_id);
