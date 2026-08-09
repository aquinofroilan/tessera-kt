-- Platform: polymorphic attachments.
-- Any module can reference an attachment by entity_type + entity_id without
-- the attachments table needing a FK per module. The storage_key column is
-- the opaque identifier the backend uses to retrieve the bytes (a relative
-- filesystem path in the current local backend; future S3 backend would use
-- the same column for the object key).

CREATE TABLE attachments (
    id              uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(uuid),
    entity_type     text NOT NULL,
    entity_id       uuid NOT NULL,
    filename        text NOT NULL,
    mime_type       text NOT NULL,
    size_bytes      bigint NOT NULL,
    storage_key     text NOT NULL,
    uploaded_by     uuid NOT NULL REFERENCES users(uuid),
    created_at      timestamp NOT NULL DEFAULT current_timestamp,
    CONSTRAINT attachments_size_chk CHECK (size_bytes >= 0)
);
CREATE INDEX attachments_org_entity_idx ON attachments(organization_id, entity_type, entity_id);
