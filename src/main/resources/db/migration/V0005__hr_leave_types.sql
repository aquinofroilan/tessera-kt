-- HR module: leave types.
CREATE TABLE leave_types (
    id                  uuid PRIMARY KEY,
    code                text NOT NULL,
    name                text NOT NULL,
    paid                boolean NOT NULL DEFAULT true,
    default_annual_days int NOT NULL DEFAULT 0,
    organization_id     uuid NOT NULL REFERENCES organizations(uuid),
    is_active           boolean NOT NULL DEFAULT true,
    created_at          timestamp NOT NULL DEFAULT current_timestamp,
    updated_at          timestamp,
    CONSTRAINT leave_types_unique_code_per_org UNIQUE (organization_id, code)
);
CREATE INDEX leave_types_org_active_idx ON leave_types(organization_id, is_active);
