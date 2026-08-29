-- Add billing plan to organizations with plan tier constraints
ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS billing_plan text NOT NULL DEFAULT 'FREE';

ALTER TABLE organizations
    ADD CONSTRAINT organizations_billing_plan_chk
    CHECK (billing_plan IN ('FREE', 'STARTER', 'ENTERPRISE'));

-- Per-organization feature flag overrides table
CREATE TABLE IF NOT EXISTS organization_feature_flags (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(uuid) ON DELETE CASCADE,
    feature_key text NOT NULL,
    enabled boolean NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_org_feature_flags_org_key UNIQUE (organization_id, feature_key)
);

CREATE INDEX IF NOT EXISTS idx_org_feature_flags_org ON organization_feature_flags(organization_id);
