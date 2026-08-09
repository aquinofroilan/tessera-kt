-- Finance: bank statements & statement lines.
-- A statement is what arrives from the bank (paper, CSV, OFX). Each line is
-- one debit or credit in the bank's books. Lines start with reconciled=false
-- and get matched against journal-entry lines by the reconciliation slice.
-- Amounts are signed: positive = credit (deposit into our account), negative
-- = debit (money leaving our account). This mirrors what banks export and
-- avoids a separate type column.

CREATE TABLE finance_bank_statements (
    id                uuid PRIMARY KEY,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    bank_account_id   uuid NOT NULL REFERENCES finance_bank_accounts(id) ON DELETE RESTRICT,
    statement_date    date NOT NULL,
    opening_balance   numeric(18,4) NOT NULL,
    closing_balance   numeric(18,4) NOT NULL,
    currency          char(3) NOT NULL,
    source            text NOT NULL DEFAULT 'CSV',
    uploaded_by       uuid NOT NULL REFERENCES users(uuid),
    notes             text,
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT finance_bank_statements_source_chk CHECK (source IN ('CSV','OFX','MANUAL'))
);
CREATE INDEX finance_bank_statements_org_account_idx
    ON finance_bank_statements(organization_id, bank_account_id, statement_date DESC);

CREATE TABLE finance_bank_statement_lines (
    id                          uuid PRIMARY KEY,
    statement_id                uuid NOT NULL REFERENCES finance_bank_statements(id) ON DELETE CASCADE,
    line_number                 int NOT NULL,
    posted_date                 date NOT NULL,
    description                 text NOT NULL,
    reference                   text,
    amount                      numeric(18,4) NOT NULL,
    reconciled                  boolean NOT NULL DEFAULT false,
    reconciled_journal_entry_id uuid,
    reconciled_at               timestamp,
    reconciled_by               uuid REFERENCES users(uuid),
    CONSTRAINT finance_bank_statement_lines_unique_per_stmt UNIQUE (statement_id, line_number),
    CONSTRAINT finance_bank_statement_lines_amount_chk CHECK (amount <> 0)
);
CREATE INDEX finance_bank_statement_lines_unreconciled_idx
    ON finance_bank_statement_lines(statement_id)
    WHERE reconciled = false;
