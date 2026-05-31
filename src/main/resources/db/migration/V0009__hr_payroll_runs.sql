-- HR module: payroll runs and their posting to the general ledger.
CREATE TABLE payroll_runs (
    id                          uuid PRIMARY KEY,
    run_number                  text NOT NULL,
    period_start                date NOT NULL,
    period_end                  date NOT NULL,
    pay_date                    date NOT NULL,
    organization_id             uuid NOT NULL REFERENCES organizations(uuid),
    status                      text NOT NULL DEFAULT 'DRAFT',
    total_gross                 numeric(18,4) NOT NULL,
    currency                    char(3) NOT NULL REFERENCES currencies(code),
    created_by                  uuid NOT NULL REFERENCES users(uuid),
    accrual_journal_entry_id    uuid REFERENCES journal_entries(id) ON DELETE RESTRICT,
    payment_journal_entry_id    uuid REFERENCES journal_entries(id) ON DELETE RESTRICT,
    approved_at                 timestamp,
    approved_by                 uuid REFERENCES users(uuid),
    paid_at                     timestamp,
    cancelled_at                timestamp,
    created_at                  timestamp NOT NULL DEFAULT current_timestamp,
    updated_at                  timestamp,
    CONSTRAINT payroll_runs_status_chk CHECK (status IN ('DRAFT','APPROVED','PAID','CANCELLED')),
    CONSTRAINT payroll_runs_unique_number_per_org UNIQUE (organization_id, run_number)
);
CREATE INDEX payroll_runs_org_status_idx ON payroll_runs(organization_id, status);

CREATE TABLE payroll_run_lines (
    id                uuid PRIMARY KEY,
    payroll_run_id    uuid NOT NULL REFERENCES payroll_runs(id) ON DELETE CASCADE,
    line_number       int NOT NULL,
    employee_id       uuid NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    employee_number   text NOT NULL,
    employee_name     text NOT NULL,
    compensation_id   uuid NOT NULL REFERENCES employee_compensation(id) ON DELETE RESTRICT,
    gross_amount      numeric(18,4) NOT NULL,
    CONSTRAINT payroll_run_lines_unique_line_per_run UNIQUE (payroll_run_id, line_number)
);
CREATE INDEX payroll_run_lines_employee_idx ON payroll_run_lines(employee_id);
