CREATE TABLE notification_webhooks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    url VARCHAR(1024) NOT NULL,
    secret VARCHAR(255),
    event_types VARCHAR(1024) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_notification_webhooks_org ON notification_webhooks(organization_id);
