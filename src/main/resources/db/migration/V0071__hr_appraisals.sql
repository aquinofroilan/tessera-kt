CREATE TABLE appraisal_cycles (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE appraisals (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    cycle_id UUID NOT NULL REFERENCES appraisal_cycles(id),
    employee_id UUID NOT NULL REFERENCES employees(id),
    manager_id UUID REFERENCES employees(id),
    status VARCHAR(50) NOT NULL,
    overall_rating DOUBLE PRECISION,
    manager_summary TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE appraisal_goals (
    id UUID PRIMARY KEY,
    appraisal_id UUID NOT NULL REFERENCES appraisals(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    weight INT NOT NULL DEFAULT 0,
    self_rating INT,
    manager_rating INT,
    self_comments TEXT,
    manager_comments TEXT
);

CREATE INDEX idx_appraisal_cycles_org ON appraisal_cycles(organization_id);
CREATE INDEX idx_appraisals_org_cycle ON appraisals(organization_id, cycle_id);
CREATE INDEX idx_appraisals_employee ON appraisals(employee_id);
