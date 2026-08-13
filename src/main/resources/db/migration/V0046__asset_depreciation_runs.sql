-- Monthly depreciation runs. One row per (org, year, month) — uniquely
-- constrained so two postings can't double-depreciate. Lines carry the
-- per-asset breakdown for audit; the posted JournalEntry aggregates the
-- amounts by debit/credit account pair.
CREATE TABLE asset_depreciation_runs (
    id                    uuid PRIMARY KEY,
    organization_id       uuid NOT NULL REFERENCES organizations(uuid),
    period_year           integer NOT NULL,
    period_month          integer NOT NULL,
    status                text NOT NULL DEFAULT 'DRAFT',
    total_depreciation    numeric(19, 4) NOT NULL DEFAULT 0,
    journal_entry_id      uuid REFERENCES journal_entries(id),
    posted_at             timestamp,
    posted_by             uuid,
    created_at            timestamp NOT NULL DEFAULT current_timestamp,
    updated_at            timestamp,
    CONSTRAINT asset_depreciation_runs_status_chk
        CHECK (status IN ('DRAFT', 'POSTED', 'CANCELLED')),
    CONSTRAINT asset_depreciation_runs_month_chk
        CHECK (period_month BETWEEN 1 AND 12),
    CONSTRAINT asset_depreciation_runs_unique_period
        UNIQUE (organization_id, period_year, period_month)
);
CREATE INDEX asset_depreciation_runs_org_status_idx
    ON asset_depreciation_runs(organization_id, status);

CREATE TABLE asset_depreciation_run_lines (
    id                          uuid PRIMARY KEY,
    run_id                      uuid NOT NULL REFERENCES asset_depreciation_runs(id) ON DELETE CASCADE,
    asset_id                    uuid NOT NULL REFERENCES fixed_assets(id),
    depreciation_amount         numeric(19, 4) NOT NULL,
    debit_account_id            uuid REFERENCES accounts(id),
    credit_account_id           uuid REFERENCES accounts(id),
    CONSTRAINT asset_depreciation_run_lines_amount_chk CHECK (depreciation_amount >= 0)
);
CREATE INDEX asset_depreciation_run_lines_run_idx
    ON asset_depreciation_run_lines(run_id);
CREATE INDEX asset_depreciation_run_lines_asset_idx
    ON asset_depreciation_run_lines(asset_id);
