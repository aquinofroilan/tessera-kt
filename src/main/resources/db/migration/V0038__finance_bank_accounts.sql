-- Finance: bank & cash accounts.
-- A bank account is the operational mirror of a 10xx Cash account in the
-- chart of accounts. Cash movements in/out get journal-entered against the
-- linked gl_account_id, so the GL stays in sync with the actual bank balance.
-- We deliberately store only the last 4 digits of the account number to keep
-- PII out of the database while still letting users disambiguate between
-- multiple accounts at the same bank.

CREATE TABLE finance_bank_accounts (
    id                       uuid PRIMARY KEY,
    organization_id          uuid NOT NULL REFERENCES organizations(uuid),
    code                     text NOT NULL,
    name                     text NOT NULL,
    bank_name                text,
    account_number_last4     text,
    currency                 char(3) NOT NULL,
    gl_account_id            uuid NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    opening_balance          numeric(18,4) NOT NULL DEFAULT 0,
    current_balance          numeric(18,4) NOT NULL DEFAULT 0,
    is_active                boolean NOT NULL DEFAULT true,
    notes                    text,
    created_by               uuid NOT NULL REFERENCES users(uuid),
    created_at               timestamp NOT NULL DEFAULT current_timestamp,
    updated_at               timestamp,
    CONSTRAINT finance_bank_accounts_unique_code_per_org UNIQUE (organization_id, code),
    CONSTRAINT finance_bank_accounts_last4_chk
        CHECK (account_number_last4 IS NULL OR length(account_number_last4) = 4)
);
CREATE INDEX finance_bank_accounts_org_active_idx ON finance_bank_accounts(organization_id, is_active);
CREATE INDEX finance_bank_accounts_org_gl_idx    ON finance_bank_accounts(organization_id, gl_account_id);
