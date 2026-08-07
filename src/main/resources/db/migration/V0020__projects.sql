-- Project module: projects (root entity of the project-management epic).
-- Projects own tasks, timesheets, budgets and billing in later migrations.
CREATE TABLE projects (
    id                   uuid PRIMARY KEY,
    project_number       text NOT NULL,
    name                 text NOT NULL,
    description          text,
    customer_id          uuid REFERENCES customers(id) ON DELETE SET NULL,
    manager_employee_id  uuid REFERENCES employees(id) ON DELETE SET NULL,
    start_date           date NOT NULL,
    end_date             date,
    status               text NOT NULL DEFAULT 'PLANNED',
    billing_type         text NOT NULL DEFAULT 'TIME_AND_MATERIALS',
    organization_id      uuid NOT NULL REFERENCES organizations(uuid),
    created_at           timestamp NOT NULL DEFAULT current_timestamp,
    updated_at           timestamp,
    CONSTRAINT projects_status_chk
        CHECK (status IN ('PLANNED','ACTIVE','ON_HOLD','CLOSED','CANCELLED')),
    CONSTRAINT projects_billing_type_chk
        CHECK (billing_type IN ('TIME_AND_MATERIALS','FIXED_PRICE','MILESTONE')),
    CONSTRAINT projects_unique_number_per_org UNIQUE (organization_id, project_number)
);
CREATE INDEX projects_org_status_idx ON projects(organization_id, status);
CREATE INDEX projects_org_customer_idx ON projects(organization_id, customer_id);
