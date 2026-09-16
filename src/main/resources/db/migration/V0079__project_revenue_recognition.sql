ALTER TABLE projects
ADD COLUMN contract_amount DECIMAL,
ADD COLUMN revenue_recognition_method VARCHAR(50);

CREATE TABLE project_revenue_recognitions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    project_id UUID NOT NULL REFERENCES projects(id),
    recognized_date DATE NOT NULL,
    recognized_revenue DECIMAL NOT NULL,
    recognized_cost DECIMAL NOT NULL,
    percent_complete DECIMAL,
    journal_entry_id UUID,
    created_by UUID NOT NULL REFERENCES users(uuid),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_project_revenue_recognitions_project ON project_revenue_recognitions(project_id);
CREATE INDEX idx_project_revenue_recognitions_org ON project_revenue_recognitions(organization_id);
