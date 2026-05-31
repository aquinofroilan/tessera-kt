-- HR module: effective-dated employee compensation records.
CREATE TABLE employee_compensation (
    id                uuid PRIMARY KEY,
    employee_id       uuid NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    position_id       uuid REFERENCES positions(id) ON DELETE SET NULL,
    pay_rate          numeric(18,4) NOT NULL,
    currency          char(3) NOT NULL REFERENCES currencies(code),
    pay_period        text NOT NULL,
    effective_date    date NOT NULL,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    created_by        uuid NOT NULL REFERENCES users(uuid),
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    CONSTRAINT employee_compensation_pay_period_chk CHECK (pay_period IN ('ANNUAL','MONTHLY','HOURLY'))
);
CREATE INDEX employee_compensation_org_employee_idx
    ON employee_compensation(organization_id, employee_id, effective_date DESC);
