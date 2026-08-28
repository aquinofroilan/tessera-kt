-- Ensure organization status values strictly follow the state machine lifecycle
ALTER TABLE organizations
    ADD CONSTRAINT organizations_status_chk
    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED'));
