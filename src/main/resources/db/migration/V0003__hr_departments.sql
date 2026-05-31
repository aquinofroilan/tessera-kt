-- HR module: departments.
CREATE TABLE departments (
    id                uuid PRIMARY KEY,
    code              text NOT NULL,
    name              text NOT NULL,
    description       text,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    is_active         boolean NOT NULL DEFAULT true,
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT departments_unique_code_per_org UNIQUE (organization_id, code)
);
CREATE INDEX departments_org_active_idx ON departments(organization_id, is_active);
