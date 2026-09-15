CREATE TABLE training_courses (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    provider VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE training_records (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    employee_id UUID NOT NULL REFERENCES employees(id),
    course_id UUID NOT NULL REFERENCES training_courses(id),
    completion_date DATE NOT NULL,
    attachment_id UUID REFERENCES attachments(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE certifications (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    employee_id UUID NOT NULL REFERENCES employees(id),
    name VARCHAR(255) NOT NULL,
    issuing_body VARCHAR(255),
    issue_date DATE NOT NULL,
    expiry_date DATE,
    credential_id VARCHAR(255),
    attachment_id UUID REFERENCES attachments(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_training_courses_org ON training_courses(organization_id);
CREATE INDEX idx_training_records_org_employee ON training_records(organization_id, employee_id);
CREATE INDEX idx_certifications_org_employee ON certifications(organization_id, employee_id);
CREATE INDEX idx_certifications_org_expiry ON certifications(organization_id, expiry_date);
