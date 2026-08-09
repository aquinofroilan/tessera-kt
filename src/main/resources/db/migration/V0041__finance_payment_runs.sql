-- Finance: batch payment runs.
-- A payment run is a batch of bills selected to be paid out of a single
-- bank account on a given date. Status flows DRAFT -> APPROVED -> EXECUTED
-- (or CANCELLED). EXECUTED means each line has been turned into a
-- bill-payment record + journal entry; the bank account's cached current
-- balance has been decremented accordingly.

CREATE TABLE finance_payment_runs (
    id                uuid PRIMARY KEY,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    code              text NOT NULL,
    bank_account_id   uuid NOT NULL REFERENCES finance_bank_accounts(id) ON DELETE RESTRICT,
    run_date          date NOT NULL,
    status            text NOT NULL DEFAULT 'DRAFT',
    total_amount      numeric(18,4) NOT NULL DEFAULT 0,
    currency          char(3) NOT NULL,
    notes             text,
    created_by        uuid NOT NULL REFERENCES users(uuid),
    approved_at       timestamp,
    approved_by       uuid REFERENCES users(uuid),
    executed_at       timestamp,
    executed_by       uuid REFERENCES users(uuid),
    cancelled_at      timestamp,
    cancelled_by      uuid REFERENCES users(uuid),
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT finance_payment_runs_status_chk CHECK (status IN ('DRAFT','APPROVED','EXECUTED','CANCELLED')),
    CONSTRAINT finance_payment_runs_total_chk  CHECK (total_amount >= 0),
    CONSTRAINT finance_payment_runs_unique_code_per_org UNIQUE (organization_id, code)
);
CREATE INDEX finance_payment_runs_org_status_idx  ON finance_payment_runs(organization_id, status);
CREATE INDEX finance_payment_runs_org_account_idx ON finance_payment_runs(organization_id, bank_account_id);

CREATE TABLE finance_payment_run_lines (
    id                  uuid PRIMARY KEY,
    payment_run_id      uuid NOT NULL REFERENCES finance_payment_runs(id) ON DELETE CASCADE,
    line_number         int NOT NULL,
    bill_id             uuid NOT NULL REFERENCES bills(id) ON DELETE RESTRICT,
    vendor_id           uuid NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    vendor_name         text NOT NULL,
    bill_number         text NOT NULL,
    amount              numeric(18,4) NOT NULL,
    status              text NOT NULL DEFAULT 'PENDING',
    bill_payment_id     uuid,
    notes               text,
    CONSTRAINT finance_payment_run_lines_status_chk CHECK (status IN ('PENDING','PAID','SKIPPED','FAILED')),
    CONSTRAINT finance_payment_run_lines_amount_chk CHECK (amount > 0),
    CONSTRAINT finance_payment_run_lines_unique_line_per_run UNIQUE (payment_run_id, line_number)
);
CREATE INDEX finance_payment_run_lines_bill_idx ON finance_payment_run_lines(bill_id);
