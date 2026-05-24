-- =========================================================================
-- Loom initial schema (Postgres migration from MongoDB).
-- Native uuid PKs/FKs; numeric(18,4) money; text + CHECK for enums;
-- char(3) for currency codes; line_number ordering on embedded children.
-- =========================================================================

-- -------------------------------------------------------------------------
-- Auth / IAM
-- -------------------------------------------------------------------------

CREATE TABLE organizations (
    uuid                       uuid PRIMARY KEY,
    org_slug                   text NOT NULL UNIQUE,
    name                       text NOT NULL UNIQUE,
    description                text,
    legal_name                 text NOT NULL,
    trade_name                 text NOT NULL,
    base_currency              char(3) NOT NULL DEFAULT 'USD',
    fiscal_year_start          timestamp NOT NULL,
    timezone                   text NOT NULL,
    status                     text NOT NULL DEFAULT 'ACTIVE',
    inventory_costing_method   text NOT NULL DEFAULT 'WEIGHTED_AVERAGE',
    is_active                  boolean NOT NULL DEFAULT true,
    created_at                 timestamp NOT NULL DEFAULT current_timestamp,
    CONSTRAINT organizations_inventory_costing_method_chk
        CHECK (inventory_costing_method IN ('FIFO','WEIGHTED_AVERAGE'))
);

CREATE TABLE currencies (
    code              char(3) PRIMARY KEY,
    name              text NOT NULL,
    symbol            text NOT NULL,
    decimal_places    int NOT NULL CHECK (decimal_places >= 0)
);

-- Late FK from organizations.base_currency → currencies.code (after currencies exists)
ALTER TABLE organizations
    ADD CONSTRAINT organizations_base_currency_fk
        FOREIGN KEY (base_currency) REFERENCES currencies(code);

CREATE TABLE users (
    uuid              uuid PRIMARY KEY,
    username          text NOT NULL UNIQUE,
    email             text NOT NULL UNIQUE,
    first_name        text NOT NULL,
    last_name         text NOT NULL,
    password_hash     text NOT NULL,
    is_active         boolean NOT NULL DEFAULT true,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp
);
CREATE INDEX users_organization_id_idx ON users(organization_id);

CREATE TABLE user_role_assignments (
    id                uuid PRIMARY KEY,
    user_id           uuid NOT NULL REFERENCES users(uuid) ON DELETE CASCADE,
    role              text NOT NULL,
    organization_id   uuid REFERENCES organizations(uuid)
);
CREATE INDEX user_role_assignments_user_idx ON user_role_assignments(user_id);
CREATE INDEX user_role_assignments_org_idx ON user_role_assignments(organization_id);

CREATE TABLE roles (
    uuid              uuid PRIMARY KEY,
    name              text NOT NULL UNIQUE,
    description       text NOT NULL,
    level             text NOT NULL,
    is_default        boolean NOT NULL DEFAULT false,
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT roles_level_chk CHECK (level IN ('SYSTEM','ORGANIZATION'))
);

CREATE TABLE role_permissions (
    role_id           uuid NOT NULL REFERENCES roles(uuid) ON DELETE CASCADE,
    permission        text NOT NULL,
    PRIMARY KEY (role_id, permission)
);

CREATE TABLE invitations (
    id                uuid PRIMARY KEY,
    email             text NOT NULL,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    role              text NOT NULL,
    token_hash        text NOT NULL UNIQUE,
    invited_by        uuid NOT NULL REFERENCES users(uuid),
    status            text NOT NULL DEFAULT 'PENDING',
    expiry_at         timestamp NOT NULL,
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    CONSTRAINT invitations_status_chk CHECK (status IN ('PENDING','ACCEPTED','EXPIRED','REVOKED'))
);
CREATE INDEX invitations_email_idx ON invitations(email);
CREATE UNIQUE INDEX invitations_pending_per_email_org_uidx
    ON invitations(email, organization_id)
    WHERE status = 'PENDING';

CREATE TABLE session_tokens (
    id                uuid PRIMARY KEY,
    token             text NOT NULL UNIQUE,
    user_id           uuid NOT NULL REFERENCES users(uuid) ON DELETE CASCADE,
    organization_id   uuid REFERENCES organizations(uuid),
    expiry_at         timestamp NOT NULL,
    ip_address        text,
    user_agent        text,
    created_at        timestamp NOT NULL DEFAULT current_timestamp
);
CREATE INDEX session_tokens_expiry_idx ON session_tokens(expiry_at);

CREATE TABLE refresh_tokens (
    id                uuid PRIMARY KEY,
    token_hash        text NOT NULL UNIQUE,
    user_id           uuid NOT NULL REFERENCES users(uuid) ON DELETE CASCADE,
    session_token_id  uuid NOT NULL REFERENCES session_tokens(id) ON DELETE CASCADE,
    expiry_at         timestamp NOT NULL,
    created_at        timestamp NOT NULL DEFAULT current_timestamp
);
CREATE INDEX refresh_tokens_expiry_idx ON refresh_tokens(expiry_at);

CREATE TABLE password_reset_tokens (
    id                uuid PRIMARY KEY,
    token_hash        text NOT NULL UNIQUE,
    user_id           uuid NOT NULL REFERENCES users(uuid) ON DELETE CASCADE,
    expiry_at         timestamp NOT NULL,
    created_at        timestamp NOT NULL DEFAULT current_timestamp
);
CREATE INDEX password_reset_tokens_expiry_idx ON password_reset_tokens(expiry_at);

CREATE TABLE api_keys (
    id                uuid PRIMARY KEY,
    name              text NOT NULL,
    key_hash          text NOT NULL UNIQUE,
    key_prefix        text NOT NULL,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    created_by        uuid NOT NULL REFERENCES users(uuid),
    is_active         boolean NOT NULL DEFAULT true,
    last_used_at      timestamp,
    expires_at        timestamp,
    created_at        timestamp NOT NULL DEFAULT current_timestamp
);
CREATE INDEX api_keys_org_idx ON api_keys(organization_id);

CREATE TABLE api_key_permissions (
    api_key_id        uuid NOT NULL REFERENCES api_keys(id) ON DELETE CASCADE,
    permission        text NOT NULL,
    PRIMARY KEY (api_key_id, permission)
);

-- -------------------------------------------------------------------------
-- Reference data
-- -------------------------------------------------------------------------

CREATE TABLE exchange_rates (
    id                uuid PRIMARY KEY,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    from_currency     char(3) NOT NULL REFERENCES currencies(code),
    to_currency       char(3) NOT NULL REFERENCES currencies(code),
    rate              numeric(18,8) NOT NULL CHECK (rate > 0),
    as_of_date        date NOT NULL,
    source            text NOT NULL DEFAULT 'MANUAL',
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT exchange_rates_source_chk CHECK (source IN ('MANUAL','AUTO')),
    CONSTRAINT exchange_rates_unique_pair_per_org_per_date
        UNIQUE (organization_id, from_currency, to_currency, as_of_date)
);
CREATE INDEX exchange_rates_org_idx ON exchange_rates(organization_id);

CREATE TABLE tax_rates (
    id                uuid PRIMARY KEY,
    name              text NOT NULL,
    code              text NOT NULL,
    percentage        numeric(18,4) NOT NULL,
    authority         text NOT NULL,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    is_active         boolean NOT NULL DEFAULT true,
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT tax_rates_unique_code_per_org UNIQUE (organization_id, code)
);
CREATE INDEX tax_rates_org_idx ON tax_rates(organization_id);

CREATE TABLE tax_groups (
    id                uuid PRIMARY KEY,
    name              text NOT NULL,
    code              text NOT NULL,
    combined_rate     numeric(18,4) NOT NULL,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    is_active         boolean NOT NULL DEFAULT true,
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT tax_groups_unique_code_per_org UNIQUE (organization_id, code)
);
CREATE INDEX tax_groups_org_idx ON tax_groups(organization_id);

CREATE TABLE tax_group_rates (
    tax_group_id      uuid NOT NULL REFERENCES tax_groups(id) ON DELETE CASCADE,
    tax_rate_id       uuid NOT NULL REFERENCES tax_rates(id) ON DELETE RESTRICT,
    PRIMARY KEY (tax_group_id, tax_rate_id)
);

CREATE TABLE fiscal_years (
    id                uuid PRIMARY KEY,
    name              text NOT NULL,
    start_date        date NOT NULL,
    end_date          date NOT NULL,
    status            text NOT NULL DEFAULT 'ACTIVE',
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    closed_at         timestamp,
    closed_by         uuid REFERENCES users(uuid),
    closing_entry_id  uuid,  -- FK to journal_entries added below (forward ref)
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT fiscal_years_status_chk CHECK (status IN ('ACTIVE','CLOSED')),
    CONSTRAINT fiscal_years_unique_name_per_org UNIQUE (organization_id, name),
    CONSTRAINT fiscal_years_date_order CHECK (start_date <= end_date)
);
CREATE INDEX fiscal_years_org_idx ON fiscal_years(organization_id);

CREATE TABLE fiscal_periods (
    id                uuid PRIMARY KEY,
    fiscal_year_id    uuid NOT NULL REFERENCES fiscal_years(id) ON DELETE CASCADE,
    period_number     int NOT NULL,
    name              text NOT NULL,
    start_date        date NOT NULL,
    end_date          date NOT NULL,
    status            text NOT NULL DEFAULT 'OPEN',
    closed_at         timestamp,
    closed_by         uuid REFERENCES users(uuid),
    reopened_at       timestamp,
    reopened_by       uuid REFERENCES users(uuid),
    CONSTRAINT fiscal_periods_status_chk CHECK (status IN ('OPEN','CLOSED','REOPENED')),
    CONSTRAINT fiscal_periods_unique_period_per_year UNIQUE (fiscal_year_id, period_number),
    CONSTRAINT fiscal_periods_date_order CHECK (start_date <= end_date)
);

-- -------------------------------------------------------------------------
-- Finance
-- -------------------------------------------------------------------------

CREATE TABLE accounts (
    id                uuid PRIMARY KEY,
    code              text NOT NULL,
    name              text NOT NULL,
    description       text,
    type              text NOT NULL,
    parent_id         uuid REFERENCES accounts(id) ON DELETE RESTRICT,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    is_active         boolean NOT NULL DEFAULT true,
    is_system_account boolean NOT NULL DEFAULT false,
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT accounts_type_chk CHECK (type IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE')),
    CONSTRAINT accounts_unique_code_per_org UNIQUE (organization_id, code)
);
CREATE INDEX accounts_org_idx ON accounts(organization_id);
CREATE INDEX accounts_parent_idx ON accounts(parent_id);

CREATE TABLE journal_entries (
    id                uuid PRIMARY KEY,
    entry_number      text NOT NULL,
    date              date NOT NULL,
    description       text NOT NULL,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    status            text NOT NULL DEFAULT 'DRAFT',
    source            text NOT NULL DEFAULT 'MANUAL',
    source_reference  text,
    created_by        uuid NOT NULL REFERENCES users(uuid),
    posted_at         timestamp,
    voided_at         timestamp,
    void_reason       text,
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT journal_entries_status_chk CHECK (status IN ('DRAFT','POSTED','VOIDED')),
    CONSTRAINT journal_entries_source_chk CHECK (source IN ('MANUAL','SYSTEM')),
    CONSTRAINT journal_entries_unique_number_per_org UNIQUE (organization_id, entry_number)
);
CREATE INDEX journal_entries_org_idx ON journal_entries(organization_id);
CREATE INDEX journal_entries_org_status_date_idx ON journal_entries(organization_id, status, date);

CREATE TABLE journal_entry_lines (
    id                uuid PRIMARY KEY,
    journal_entry_id  uuid NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    line_number       int NOT NULL,
    account_id        uuid NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    account_code      text NOT NULL,
    account_name      text NOT NULL,
    debit             numeric(18,4) NOT NULL DEFAULT 0,
    credit            numeric(18,4) NOT NULL DEFAULT 0,
    description       text,
    CONSTRAINT journal_entry_lines_unique_line_per_entry UNIQUE (journal_entry_id, line_number),
    CONSTRAINT journal_entry_lines_amounts_chk CHECK (
        debit >= 0 AND credit >= 0 AND (debit = 0 OR credit = 0) AND (debit > 0 OR credit > 0)
    )
);
CREATE INDEX journal_entry_lines_account_idx ON journal_entry_lines(account_id);

-- Now add the forward FK from fiscal_years.closing_entry_id
ALTER TABLE fiscal_years
    ADD CONSTRAINT fiscal_years_closing_entry_fk
        FOREIGN KEY (closing_entry_id) REFERENCES journal_entries(id) ON DELETE RESTRICT;

CREATE TABLE vendors (
    id                            uuid PRIMARY KEY,
    name                          text NOT NULL,
    contact_name                  text,
    contact_email                 text,
    contact_phone                 text,
    payment_term_days             int NOT NULL DEFAULT 30 CHECK (payment_term_days >= 0),
    default_expense_account_id    uuid REFERENCES accounts(id) ON DELETE RESTRICT,
    organization_id               uuid NOT NULL REFERENCES organizations(uuid),
    is_active                     boolean NOT NULL DEFAULT true,
    created_at                    timestamp NOT NULL DEFAULT current_timestamp,
    updated_at                    timestamp,
    CONSTRAINT vendors_unique_name_per_org UNIQUE (organization_id, name)
);
CREATE INDEX vendors_org_idx ON vendors(organization_id);

CREATE TABLE customers (
    id                            uuid PRIMARY KEY,
    name                          text NOT NULL,
    contact_name                  text,
    contact_email                 text,
    contact_phone                 text,
    payment_term_days             int NOT NULL DEFAULT 30 CHECK (payment_term_days >= 0),
    default_revenue_account_id    uuid REFERENCES accounts(id) ON DELETE RESTRICT,
    organization_id               uuid NOT NULL REFERENCES organizations(uuid),
    is_active                     boolean NOT NULL DEFAULT true,
    created_at                    timestamp NOT NULL DEFAULT current_timestamp,
    updated_at                    timestamp,
    CONSTRAINT customers_unique_name_per_org UNIQUE (organization_id, name)
);
CREATE INDEX customers_org_idx ON customers(organization_id);

CREATE TABLE bills (
    id                              uuid PRIMARY KEY,
    bill_number                     text NOT NULL,
    vendor_id                       uuid NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    vendor_name                     text NOT NULL,
    date                            date NOT NULL,
    due_date                        date NOT NULL,
    reference_number                text,
    tax_group_id                    uuid REFERENCES tax_groups(id) ON DELETE RESTRICT,
    organization_id                 uuid NOT NULL REFERENCES organizations(uuid),
    status                          text NOT NULL DEFAULT 'DRAFT',
    total_amount                    numeric(18,4) NOT NULL,
    tax_amount                      numeric(18,4) NOT NULL DEFAULT 0,
    amount_paid                     numeric(18,4) NOT NULL DEFAULT 0,
    currency_code                   char(3) NOT NULL DEFAULT 'USD' REFERENCES currencies(code),
    exchange_rate                   numeric(18,8) NOT NULL DEFAULT 1,
    base_currency_amount            numeric(18,4) NOT NULL,
    base_currency_tax_amount        numeric(18,4) NOT NULL DEFAULT 0,
    base_currency_amount_paid       numeric(18,4) NOT NULL DEFAULT 0,
    journal_entry_id                uuid REFERENCES journal_entries(id) ON DELETE RESTRICT,
    created_by                      uuid NOT NULL REFERENCES users(uuid),
    approved_at                     timestamp,
    approved_by                     uuid REFERENCES users(uuid),
    paid_at                         timestamp,
    voided_at                       timestamp,
    voided_by                       uuid REFERENCES users(uuid),
    void_reason                     text,
    created_at                      timestamp NOT NULL DEFAULT current_timestamp,
    updated_at                      timestamp,
    CONSTRAINT bills_status_chk CHECK (status IN ('DRAFT','APPROVED','PARTIALLY_PAID','PAID','VOID')),
    CONSTRAINT bills_unique_number_per_org UNIQUE (organization_id, bill_number)
);
CREATE INDEX bills_org_status_idx ON bills(organization_id, status);
CREATE INDEX bills_org_vendor_idx ON bills(organization_id, vendor_id);

CREATE TABLE bill_lines (
    id                uuid PRIMARY KEY,
    bill_id           uuid NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
    line_number       int NOT NULL,
    account_id        uuid NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    account_code      text NOT NULL,
    account_name      text NOT NULL,
    amount            numeric(18,4) NOT NULL,
    description       text,
    CONSTRAINT bill_lines_unique_line_per_bill UNIQUE (bill_id, line_number)
);
CREATE INDEX bill_lines_account_idx ON bill_lines(account_id);

CREATE TABLE bill_payments (
    id                      uuid PRIMARY KEY,
    bill_id                 uuid NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
    payment_date            date NOT NULL,
    amount                  numeric(18,4) NOT NULL,
    base_currency_amount    numeric(18,4) NOT NULL,
    exchange_rate           numeric(18,8) NOT NULL DEFAULT 1,
    payment_method          text NOT NULL,
    reference_number        text,
    journal_entry_id        uuid REFERENCES journal_entries(id) ON DELETE RESTRICT,
    organization_id         uuid NOT NULL REFERENCES organizations(uuid),
    created_by              uuid NOT NULL REFERENCES users(uuid),
    created_at              timestamp NOT NULL DEFAULT current_timestamp,
    CONSTRAINT bill_payments_method_chk CHECK (
        payment_method IN ('CASH','CHECK','BANK_TRANSFER','CREDIT_CARD','OTHER')
    )
);
CREATE INDEX bill_payments_org_bill_idx ON bill_payments(organization_id, bill_id);

CREATE TABLE invoices (
    id                              uuid PRIMARY KEY,
    invoice_number                  text NOT NULL,
    customer_id                     uuid NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    customer_name                   text NOT NULL,
    date                            date NOT NULL,
    due_date                        date NOT NULL,
    reference_number                text,
    tax_group_id                    uuid REFERENCES tax_groups(id) ON DELETE RESTRICT,
    organization_id                 uuid NOT NULL REFERENCES organizations(uuid),
    status                          text NOT NULL DEFAULT 'DRAFT',
    total_amount                    numeric(18,4) NOT NULL,
    tax_amount                      numeric(18,4) NOT NULL DEFAULT 0,
    amount_received                 numeric(18,4) NOT NULL DEFAULT 0,
    currency_code                   char(3) NOT NULL DEFAULT 'USD' REFERENCES currencies(code),
    exchange_rate                   numeric(18,8) NOT NULL DEFAULT 1,
    base_currency_amount            numeric(18,4) NOT NULL,
    base_currency_tax_amount        numeric(18,4) NOT NULL DEFAULT 0,
    base_currency_amount_received   numeric(18,4) NOT NULL DEFAULT 0,
    journal_entry_id                uuid REFERENCES journal_entries(id) ON DELETE RESTRICT,
    created_by                      uuid NOT NULL REFERENCES users(uuid),
    approved_at                     timestamp,
    approved_by                     uuid REFERENCES users(uuid),
    paid_at                         timestamp,
    voided_at                       timestamp,
    voided_by                       uuid REFERENCES users(uuid),
    void_reason                     text,
    created_at                      timestamp NOT NULL DEFAULT current_timestamp,
    updated_at                      timestamp,
    CONSTRAINT invoices_status_chk CHECK (status IN ('DRAFT','APPROVED','PARTIALLY_PAID','PAID','VOID')),
    CONSTRAINT invoices_unique_number_per_org UNIQUE (organization_id, invoice_number)
);
CREATE INDEX invoices_org_status_idx ON invoices(organization_id, status);
CREATE INDEX invoices_org_customer_idx ON invoices(organization_id, customer_id);

CREATE TABLE invoice_lines (
    id                uuid PRIMARY KEY,
    invoice_id        uuid NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    line_number       int NOT NULL,
    account_id        uuid NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    account_code      text NOT NULL,
    account_name      text NOT NULL,
    amount            numeric(18,4) NOT NULL,
    description       text,
    CONSTRAINT invoice_lines_unique_line_per_invoice UNIQUE (invoice_id, line_number)
);
CREATE INDEX invoice_lines_account_idx ON invoice_lines(account_id);

CREATE TABLE invoice_receipts (
    id                      uuid PRIMARY KEY,
    invoice_id              uuid NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    receipt_date            date NOT NULL,
    amount                  numeric(18,4) NOT NULL,
    base_currency_amount    numeric(18,4) NOT NULL,
    exchange_rate           numeric(18,8) NOT NULL DEFAULT 1,
    payment_method          text NOT NULL,
    reference_number        text,
    journal_entry_id        uuid REFERENCES journal_entries(id) ON DELETE RESTRICT,
    organization_id         uuid NOT NULL REFERENCES organizations(uuid),
    created_by              uuid NOT NULL REFERENCES users(uuid),
    created_at              timestamp NOT NULL DEFAULT current_timestamp,
    CONSTRAINT invoice_receipts_method_chk CHECK (
        payment_method IN ('CASH','CHECK','BANK_TRANSFER','CREDIT_CARD','OTHER')
    )
);
CREATE INDEX invoice_receipts_org_invoice_idx ON invoice_receipts(organization_id, invoice_id);

-- -------------------------------------------------------------------------
-- Inventory / Product catalog
-- -------------------------------------------------------------------------

CREATE TABLE products (
    id                uuid PRIMARY KEY,
    sku               text NOT NULL,
    name              text NOT NULL,
    description       text,
    category          text,
    image_url         text,
    list_price        numeric(18,4) NOT NULL,
    price_currency    char(3) NOT NULL REFERENCES currencies(code),
    tax_group_id      uuid REFERENCES tax_groups(id) ON DELETE RESTRICT,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    is_active         boolean NOT NULL DEFAULT true,
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT products_unique_sku_per_org UNIQUE (organization_id, sku)
);
CREATE INDEX products_org_active_idx ON products(organization_id, is_active);
CREATE INDEX products_org_category_idx ON products(organization_id, category);

CREATE TABLE warehouses (
    id                    uuid PRIMARY KEY,
    code                  text NOT NULL,
    name                  text NOT NULL,
    description           text,
    address_line          text,
    city                  text,
    country               text,
    allow_negative_stock  boolean NOT NULL DEFAULT false,
    organization_id       uuid NOT NULL REFERENCES organizations(uuid),
    is_active             boolean NOT NULL DEFAULT true,
    created_at            timestamp NOT NULL DEFAULT current_timestamp,
    updated_at            timestamp,
    CONSTRAINT warehouses_unique_code_per_org UNIQUE (organization_id, code)
);
CREATE INDEX warehouses_org_active_idx ON warehouses(organization_id, is_active);

CREATE TABLE stock_movements (
    id                          uuid PRIMARY KEY,
    organization_id             uuid NOT NULL REFERENCES organizations(uuid),
    type                        text NOT NULL,
    product_id                  uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    warehouse_id                uuid NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    transfer_to_warehouse_id    uuid REFERENCES warehouses(id) ON DELETE RESTRICT,
    quantity                    numeric(18,4) NOT NULL,
    unit_cost                   numeric(18,4),
    reference                   text,
    notes                       text,
    occurred_at                 timestamp NOT NULL,
    created_by                  uuid NOT NULL REFERENCES users(uuid),
    created_at                  timestamp NOT NULL DEFAULT current_timestamp,
    CONSTRAINT stock_movements_type_chk CHECK (
        type IN ('RECEIPT','ISSUE','TRANSFER','ADJUSTMENT','OPENING_BALANCE')
    ),
    CONSTRAINT stock_movements_transfer_distinct CHECK (
        transfer_to_warehouse_id IS NULL OR transfer_to_warehouse_id <> warehouse_id
    )
);
CREATE INDEX stock_movements_org_product_warehouse_idx
    ON stock_movements(organization_id, product_id, warehouse_id);
CREATE INDEX stock_movements_org_transfer_to_idx
    ON stock_movements(organization_id, transfer_to_warehouse_id);
CREATE INDEX stock_movements_org_occurred_idx
    ON stock_movements(organization_id, occurred_at DESC);

CREATE TABLE stock_on_hand (
    id                uuid PRIMARY KEY,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    product_id        uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    warehouse_id      uuid NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    quantity          numeric(18,4) NOT NULL DEFAULT 0,
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT stock_on_hand_unique_org_product_warehouse
        UNIQUE (organization_id, product_id, warehouse_id)
);

CREATE TABLE inventory_cost_layers (
    id                  uuid PRIMARY KEY,
    organization_id     uuid NOT NULL REFERENCES organizations(uuid),
    product_id          uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    warehouse_id        uuid NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    original_quantity   numeric(18,4) NOT NULL,
    remaining_quantity  numeric(18,4) NOT NULL,
    unit_cost           numeric(18,4) NOT NULL,
    source_movement_id  uuid NOT NULL REFERENCES stock_movements(id) ON DELETE RESTRICT,
    occurred_at         timestamp NOT NULL,
    created_at          timestamp NOT NULL DEFAULT current_timestamp,
    CONSTRAINT inventory_cost_layers_qty_chk CHECK (
        original_quantity >= 0 AND remaining_quantity >= 0 AND remaining_quantity <= original_quantity
    )
);
CREATE INDEX inventory_cost_layers_org_product_warehouse_occurred_idx
    ON inventory_cost_layers(organization_id, product_id, warehouse_id, occurred_at);

CREATE TABLE inventory_wa_snapshots (
    id                uuid PRIMARY KEY,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    product_id        uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    warehouse_id      uuid NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    quantity          numeric(18,4) NOT NULL,
    total_cost        numeric(18,4) NOT NULL,
    updated_at        timestamp,
    CONSTRAINT inventory_wa_snapshots_unique_org_product_warehouse
        UNIQUE (organization_id, product_id, warehouse_id)
);

CREATE TABLE inventory_reorder_rules (
    id                uuid PRIMARY KEY,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    product_id        uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    warehouse_id      uuid NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    reorder_point     numeric(18,4) NOT NULL CHECK (reorder_point >= 0),
    safety_stock      numeric(18,4) NOT NULL DEFAULT 0 CHECK (safety_stock >= 0),
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT inventory_reorder_rules_unique_org_product_warehouse
        UNIQUE (organization_id, product_id, warehouse_id)
);
