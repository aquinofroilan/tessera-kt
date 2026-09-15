CREATE TABLE checklist_templates (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE checklist_task_templates (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES checklist_templates(id),
    description TEXT NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE employee_checklists (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    employee_id UUID NOT NULL REFERENCES employees(id),
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employee_checklist_tasks (
    id UUID PRIMARY KEY,
    checklist_id UUID NOT NULL REFERENCES employee_checklists(id),
    description TEXT NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    completed_at TIMESTAMP,
    completed_by UUID REFERENCES users(id)
);

CREATE INDEX idx_checklist_templates_org ON checklist_templates(organization_id);
CREATE INDEX idx_employee_checklists_org_employee ON employee_checklists(organization_id, employee_id);
