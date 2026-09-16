CREATE TABLE project_resource_allocations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    project_id UUID NOT NULL REFERENCES projects(id),
    employee_id UUID NOT NULL REFERENCES employees(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    allocated_hours DECIMAL NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_project_resource_allocations_project ON project_resource_allocations(project_id);
CREATE INDEX idx_project_resource_allocations_employee ON project_resource_allocations(employee_id);
CREATE INDEX idx_project_resource_allocations_org ON project_resource_allocations(organization_id);
