CREATE TABLE benefit_plans (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    employee_contribution NUMERIC(15, 2) NOT NULL DEFAULT 0,
    employer_contribution NUMERIC(15, 2) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE benefit_enrollments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    employee_id UUID NOT NULL REFERENCES employees(id),
    plan_id UUID NOT NULL REFERENCES benefit_plans(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (organization_id, employee_id, plan_id)
);

ALTER TABLE payroll_run_lines ADD COLUMN deductions_amount NUMERIC(15, 2) NOT NULL DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN net_amount NUMERIC(15, 2) NOT NULL DEFAULT 0;

ALTER TABLE payroll_runs ADD COLUMN total_deductions NUMERIC(15, 2) NOT NULL DEFAULT 0;
ALTER TABLE payroll_runs ADD COLUMN total_net NUMERIC(15, 2) NOT NULL DEFAULT 0;

-- Backfill net amount for existing payroll lines and runs
UPDATE payroll_run_lines SET net_amount = gross_amount;
UPDATE payroll_runs SET total_net = total_gross;
