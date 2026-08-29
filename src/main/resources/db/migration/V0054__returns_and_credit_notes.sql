-- Sales returns (RMA)
CREATE TABLE IF NOT EXISTS sales_returns (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(uuid) ON DELETE CASCADE,
    return_number text NOT NULL,
    customer_id uuid NOT NULL REFERENCES customers(id),
    customer_name text NOT NULL,
    sales_order_id uuid REFERENCES sales_orders(id) ON DELETE SET NULL,
    invoice_id uuid REFERENCES invoices(id) ON DELETE SET NULL,
    warehouse_id uuid NOT NULL REFERENCES warehouses(id),
    return_date date NOT NULL,
    status text NOT NULL,
    reason text NOT NULL,
    notes text,
    restock_inventory boolean NOT NULL DEFAULT true,
    total_amount numeric(19, 4) NOT NULL,
    created_by uuid NOT NULL,
    approved_by uuid,
    approved_at timestamp with time zone,
    received_by uuid,
    received_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_sales_returns_org_number UNIQUE (organization_id, return_number),
    CONSTRAINT chk_sales_return_status CHECK (status IN ('REQUESTED', 'APPROVED', 'RECEIVED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_sales_return_reason CHECK (reason IN ('DEFECTIVE', 'WRONG_ITEM', 'BUYER_REMORSE', 'DAMAGED_IN_TRANSIT', 'EXPIRED', 'OTHER'))
);

CREATE INDEX IF NOT EXISTS idx_sales_returns_org ON sales_returns(organization_id);
CREATE INDEX IF NOT EXISTS idx_sales_returns_cust ON sales_returns(customer_id);
CREATE INDEX IF NOT EXISTS idx_sales_returns_so ON sales_returns(sales_order_id);

CREATE TABLE IF NOT EXISTS sales_return_lines (
    id uuid PRIMARY KEY,
    sales_return_id uuid NOT NULL REFERENCES sales_returns(id) ON DELETE CASCADE,
    line_number integer NOT NULL,
    product_id uuid NOT NULL REFERENCES products(id),
    product_sku text NOT NULL,
    product_name text NOT NULL,
    quantity numeric(19, 4) NOT NULL,
    unit_price numeric(19, 4) NOT NULL,
    line_total numeric(19, 4) NOT NULL,
    received_quantity numeric(19, 4) NOT NULL DEFAULT 0,
    condition_notes text,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sales_return_lines_ret ON sales_return_lines(sales_return_id);

-- Credit Notes
CREATE TABLE IF NOT EXISTS credit_notes (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(uuid) ON DELETE CASCADE,
    credit_note_number text NOT NULL,
    customer_id uuid NOT NULL REFERENCES customers(id),
    customer_name text NOT NULL,
    sales_return_id uuid REFERENCES sales_returns(id) ON DELETE SET NULL,
    invoice_id uuid REFERENCES invoices(id) ON DELETE SET NULL,
    date date NOT NULL,
    currency text NOT NULL DEFAULT 'USD',
    total_amount numeric(19, 4) NOT NULL,
    allocated_amount numeric(19, 4) NOT NULL DEFAULT 0,
    status text NOT NULL,
    reason text,
    created_by uuid NOT NULL,
    approved_by uuid,
    approved_at timestamp with time zone,
    journal_entry_id uuid,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_credit_notes_org_number UNIQUE (organization_id, credit_note_number),
    CONSTRAINT chk_credit_note_status CHECK (status IN ('DRAFT', 'APPROVED', 'PARTIALLY_APPLIED', 'APPLIED', 'VOID'))
);

CREATE INDEX IF NOT EXISTS idx_credit_notes_org ON credit_notes(organization_id);
CREATE INDEX IF NOT EXISTS idx_credit_notes_cust ON credit_notes(customer_id);
CREATE INDEX IF NOT EXISTS idx_credit_notes_ret ON credit_notes(sales_return_id);

CREATE TABLE IF NOT EXISTS credit_note_lines (
    id uuid PRIMARY KEY,
    credit_note_id uuid NOT NULL REFERENCES credit_notes(id) ON DELETE CASCADE,
    line_number integer NOT NULL,
    product_id uuid REFERENCES products(id) ON DELETE SET NULL,
    description text NOT NULL,
    quantity numeric(19, 4) NOT NULL DEFAULT 1,
    unit_price numeric(19, 4) NOT NULL,
    line_total numeric(19, 4) NOT NULL,
    account_id uuid,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_credit_note_lines_cn ON credit_note_lines(credit_note_id);

CREATE TABLE IF NOT EXISTS credit_note_allocations (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(uuid) ON DELETE CASCADE,
    credit_note_id uuid NOT NULL REFERENCES credit_notes(id) ON DELETE CASCADE,
    invoice_id uuid NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    amount_applied numeric(19, 4) NOT NULL,
    applied_date date NOT NULL,
    applied_by uuid NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_credit_note_allocations_cn ON credit_note_allocations(credit_note_id);
CREATE INDEX IF NOT EXISTS idx_credit_note_allocations_inv ON credit_note_allocations(invoice_id);
