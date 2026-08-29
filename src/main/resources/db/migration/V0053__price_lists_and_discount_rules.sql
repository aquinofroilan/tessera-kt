-- Add customer segment and default price list to customers table
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS customer_segment text NOT NULL DEFAULT 'RETAIL',
    ADD COLUMN IF NOT EXISTS default_price_list_id uuid;

-- Price lists table
CREATE TABLE IF NOT EXISTS price_lists (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(uuid) ON DELETE CASCADE,
    name text NOT NULL,
    code text NOT NULL,
    currency text NOT NULL,
    customer_segment text,
    is_default boolean NOT NULL DEFAULT false,
    is_active boolean NOT NULL DEFAULT true,
    valid_from date,
    valid_to date,
    description text,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_price_lists_org_code UNIQUE (organization_id, code)
);

CREATE INDEX IF NOT EXISTS idx_price_lists_org ON price_lists(organization_id);
CREATE INDEX IF NOT EXISTS idx_price_lists_org_curr_seg ON price_lists(organization_id, currency, customer_segment);

-- Price list lines table
CREATE TABLE IF NOT EXISTS price_list_lines (
    id uuid PRIMARY KEY,
    price_list_id uuid NOT NULL REFERENCES price_lists(id) ON DELETE CASCADE,
    product_id uuid NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    product_sku text NOT NULL,
    unit_price numeric(19, 4) NOT NULL,
    min_quantity numeric(19, 4) NOT NULL DEFAULT 1.0,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_price_list_lines_item_qty UNIQUE (price_list_id, product_id, min_quantity)
);

CREATE INDEX IF NOT EXISTS idx_price_list_lines_plist ON price_list_lines(price_list_id);
CREATE INDEX IF NOT EXISTS idx_price_list_lines_prod ON price_list_lines(product_id);

-- Discount rules table
CREATE TABLE IF NOT EXISTS discount_rules (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(uuid) ON DELETE CASCADE,
    name text NOT NULL,
    code text NOT NULL,
    discount_type text NOT NULL,
    discount_value numeric(19, 4) NOT NULL,
    customer_segment text,
    customer_id uuid REFERENCES customers(id) ON DELETE CASCADE,
    product_id uuid REFERENCES products(id) ON DELETE CASCADE,
    price_list_id uuid REFERENCES price_lists(id) ON DELETE CASCADE,
    min_quantity numeric(19, 4),
    min_order_amount numeric(19, 4),
    valid_from date,
    valid_to date,
    is_active boolean NOT NULL DEFAULT true,
    priority integer NOT NULL DEFAULT 0,
    description text,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_discount_rules_org_code UNIQUE (organization_id, code),
    CONSTRAINT chk_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'VOLUME_TIER'))
);

CREATE INDEX IF NOT EXISTS idx_discount_rules_org ON discount_rules(organization_id);
CREATE INDEX IF NOT EXISTS idx_discount_rules_cust ON discount_rules(customer_id);
CREATE INDEX IF NOT EXISTS idx_discount_rules_prod ON discount_rules(product_id);
