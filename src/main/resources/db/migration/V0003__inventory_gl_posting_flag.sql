-- Opt-in flag for posting inventory movements (receipts, issues/COGS, adjustments)
-- to the General Ledger. Defaults to false to preserve existing behavior; when
-- enabled, the org must have the inventory posting accounts configured.
ALTER TABLE organizations
    ADD COLUMN inventory_gl_posting_enabled boolean NOT NULL DEFAULT false;
