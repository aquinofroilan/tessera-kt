CREATE TABLE scheduled_reports (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(255) NOT NULL,
    report_type VARCHAR(255) NOT NULL,
    format VARCHAR(50) NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    recipient_emails JSONB NOT NULL,
    query_params JSONB,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_scheduled_reports_org_id ON scheduled_reports(organization_id);
