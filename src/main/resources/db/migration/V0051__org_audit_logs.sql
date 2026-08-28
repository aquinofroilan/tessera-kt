-- Create organization audit logs table
CREATE TABLE IF NOT EXISTS organization_audit_logs (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(uuid) ON DELETE CASCADE,
    actor_id uuid,
    actor_name text,
    action text NOT NULL,
    category text NOT NULL,
    entity_type text NOT NULL,
    entity_id text,
    old_value text,
    new_value text,
    ip_address text,
    user_agent text,
    created_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_org_audit_logs_org_created ON organization_audit_logs(organization_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_org_audit_logs_category ON organization_audit_logs(organization_id, category);
CREATE INDEX IF NOT EXISTS idx_org_audit_logs_action ON organization_audit_logs(organization_id, action);
