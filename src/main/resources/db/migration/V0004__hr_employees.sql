-- HR module: employees.
CREATE TABLE employees (
    id                uuid PRIMARY KEY,
    employee_number   text NOT NULL,
    first_name        text NOT NULL,
    last_name         text NOT NULL,
    email             text,
    job_title         text,
    department_id     uuid REFERENCES departments(id) ON DELETE SET NULL,
    hire_date         date NOT NULL,
    status            text NOT NULL DEFAULT 'ACTIVE',
    termination_date  date,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT employees_status_chk CHECK (status IN ('ACTIVE','ON_LEAVE','TERMINATED')),
    CONSTRAINT employees_unique_number_per_org UNIQUE (organization_id, employee_number)
);
CREATE INDEX employees_org_status_idx ON employees(organization_id, status);
CREATE INDEX employees_org_department_idx ON employees(organization_id, department_id);
