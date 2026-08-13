-- Asset disposal lifecycle. One row per disposal event; an asset can only
-- be disposed once (status flips to DISPOSED on post). The JE generated
-- by posting removes the gross cost + accumulated depreciation from the
-- books, recognises proceeds (if any), and posts the net gain or loss
-- to the configured gain/loss account.
CREATE TABLE asset_disposals (
    id                       uuid PRIMARY KEY,
    organization_id          uuid NOT NULL REFERENCES organizations(uuid),
    asset_id                 uuid NOT NULL REFERENCES fixed_assets(id),
    disposal_date            date NOT NULL,
    disposal_type            text NOT NULL,
    proceeds                 numeric(19, 4) NOT NULL DEFAULT 0,
    status                   text NOT NULL DEFAULT 'DRAFT',
    journal_entry_id         uuid REFERENCES journal_entries(id),
    gain_loss_account_id     uuid REFERENCES accounts(id),
    cash_account_id          uuid REFERENCES accounts(id),
    notes                    text,
    posted_at                timestamp,
    posted_by                uuid,
    created_at               timestamp NOT NULL DEFAULT current_timestamp,
    updated_at               timestamp,
    CONSTRAINT asset_disposals_type_chk
        CHECK (disposal_type IN ('SALE', 'WRITE_OFF', 'SCRAP')),
    CONSTRAINT asset_disposals_status_chk
        CHECK (status IN ('DRAFT', 'POSTED', 'CANCELLED')),
    CONSTRAINT asset_disposals_proceeds_chk CHECK (proceeds >= 0)
);
CREATE INDEX asset_disposals_org_asset_idx ON asset_disposals(organization_id, asset_id);
CREATE INDEX asset_disposals_org_status_idx ON asset_disposals(organization_id, status);
