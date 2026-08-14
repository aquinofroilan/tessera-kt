-- Fixed Assets foundation. Two tables today; depreciation runs and
-- disposal events get their own tables in follow-up migrations.
--
-- Each asset carries three GL account references that the upcoming
-- depreciation-run posting code will use:
--   asset_account                — the 1500-series gross asset account
--   accumulated_depreciation_account — the 1599-series contra account
--   depreciation_expense_account — the 6900-series expense account
CREATE TABLE asset_categories (
    id                              uuid PRIMARY KEY,
    organization_id                 uuid NOT NULL REFERENCES organizations(uuid),
    code                            text NOT NULL,
    name                            text NOT NULL,
    description                     text,
    default_useful_life_months      integer,
    default_depreciation_method     text NOT NULL DEFAULT 'STRAIGHT_LINE',
    default_salvage_value           numeric(19, 4) NOT NULL DEFAULT 0,
    is_active                       boolean NOT NULL DEFAULT true,
    created_at                      timestamp NOT NULL DEFAULT current_timestamp,
    updated_at                      timestamp,
    CONSTRAINT asset_categories_method_chk
        CHECK (default_depreciation_method IN ('STRAIGHT_LINE')),
    CONSTRAINT asset_categories_unique_code UNIQUE (organization_id, code)
);
CREATE INDEX asset_categories_org_idx ON asset_categories(organization_id);

CREATE TABLE fixed_assets (
    id                                       uuid PRIMARY KEY,
    organization_id                          uuid NOT NULL REFERENCES organizations(uuid),
    asset_number                             text NOT NULL,
    name                                     text NOT NULL,
    description                              text,
    category_id                              uuid REFERENCES asset_categories(id) ON DELETE SET NULL,
    acquisition_date                         date NOT NULL,
    acquisition_cost                         numeric(19, 4) NOT NULL,
    salvage_value                            numeric(19, 4) NOT NULL DEFAULT 0,
    useful_life_months                       integer NOT NULL,
    depreciation_method                      text NOT NULL DEFAULT 'STRAIGHT_LINE',
    location                                 text,
    serial_number                            text,
    status                                   text NOT NULL DEFAULT 'ACTIVE',
    accumulated_depreciation                 numeric(19, 4) NOT NULL DEFAULT 0,
    asset_account_id                         uuid REFERENCES accounts(id),
    accumulated_depreciation_account_id      uuid REFERENCES accounts(id),
    depreciation_expense_account_id          uuid REFERENCES accounts(id),
    created_at                               timestamp NOT NULL DEFAULT current_timestamp,
    updated_at                               timestamp,
    CONSTRAINT fixed_assets_status_chk
        CHECK (status IN ('ACTIVE', 'DISPOSED', 'FULLY_DEPRECIATED')),
    CONSTRAINT fixed_assets_method_chk
        CHECK (depreciation_method IN ('STRAIGHT_LINE')),
    CONSTRAINT fixed_assets_useful_life_chk CHECK (useful_life_months > 0),
    CONSTRAINT fixed_assets_cost_chk CHECK (acquisition_cost >= 0),
    CONSTRAINT fixed_assets_salvage_chk CHECK (salvage_value >= 0),
    CONSTRAINT fixed_assets_unique_number UNIQUE (organization_id, asset_number)
);
CREATE INDEX fixed_assets_org_status_idx ON fixed_assets(organization_id, status);
CREATE INDEX fixed_assets_org_category_idx ON fixed_assets(organization_id, category_id);
