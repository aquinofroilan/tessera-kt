-- Project module: timesheets / time entries.
-- Employees log time against a project (and optionally a task). Distinct from
-- HR attendance. Approved billable entries feed project budgeting and billing.
CREATE TABLE time_entries (
    id               uuid PRIMARY KEY,
    employee_id      uuid NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    project_id       uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    task_id          uuid REFERENCES project_tasks(id) ON DELETE SET NULL,
    entry_date       date NOT NULL,
    hours            numeric(8,2) NOT NULL,
    billable         boolean NOT NULL DEFAULT true,
    rate             numeric(18,4),
    status           text NOT NULL DEFAULT 'DRAFT',
    notes            text,
    approved_by      uuid REFERENCES users(uuid),
    approved_at      timestamp,
    organization_id  uuid NOT NULL REFERENCES organizations(uuid),
    created_at       timestamp NOT NULL DEFAULT current_timestamp,
    updated_at       timestamp,
    CONSTRAINT time_entries_status_chk
        CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','REJECTED'))
);
CREATE INDEX time_entries_org_project_idx ON time_entries(organization_id, project_id);
CREATE INDEX time_entries_org_employee_idx ON time_entries(organization_id, employee_id);
CREATE INDEX time_entries_org_status_idx ON time_entries(organization_id, status);
