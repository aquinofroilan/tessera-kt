-- HR module: position / job catalog.
CREATE TABLE positions (
    id                uuid PRIMARY KEY,
    code              text NOT NULL,
    title             text NOT NULL,
    department_id     uuid REFERENCES departments(id) ON DELETE SET NULL,
    pay_grade         text,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    is_active         boolean NOT NULL DEFAULT true,
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT positions_unique_code_per_org UNIQUE (organization_id, code)
);
CREATE INDEX positions_org_active_idx ON positions(organization_id, is_active);
