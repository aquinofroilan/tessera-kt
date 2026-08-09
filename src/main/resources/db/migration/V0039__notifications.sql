-- Generic in-app notification feed. Any service can write a row; the
-- recipient reads their own via the /notifications endpoints.
--
-- Sub-PR 1 of the Notifications & Workflow stack (epic #13). Subsequent
-- sub-PRs add: an email delivery channel, per-user preferences, hooks
-- into existing approve/reject methods, and a configurable rules engine.
CREATE TABLE notifications (
    id               uuid PRIMARY KEY,
    organization_id  uuid NOT NULL REFERENCES organizations(uuid),
    recipient_user_id uuid NOT NULL REFERENCES users(uuid) ON DELETE CASCADE,
    category         text NOT NULL,
    kind             text NOT NULL,
    title            text NOT NULL,
    body             text,
    link             text,
    read_at          timestamp,
    created_at       timestamp NOT NULL DEFAULT current_timestamp,
    CONSTRAINT notifications_category_chk
        CHECK (category IN ('SYSTEM','APPROVAL','REMINDER','EVENT','INFO'))
);
CREATE INDEX notifications_recipient_unread_idx
    ON notifications(recipient_user_id, read_at NULLS FIRST, created_at DESC);
CREATE INDEX notifications_org_created_idx
    ON notifications(organization_id, created_at DESC);
