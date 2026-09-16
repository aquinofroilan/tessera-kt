CREATE TABLE employee_documents (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    employee_id UUID NOT NULL REFERENCES employees(id),
    attachment_id UUID NOT NULL REFERENCES attachments(id),
    category VARCHAR(50) NOT NULL,
    expiry_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_employee_documents_org ON employee_documents(organization_id);
CREATE INDEX idx_employee_documents_emp ON employee_documents(employee_id);
CREATE INDEX idx_employee_documents_expiry ON employee_documents(expiry_date);
