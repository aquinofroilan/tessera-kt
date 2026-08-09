-- Admin-configurable rules that fan domain events out into additional
-- notifications. A rule matches when (organization_id, event_kind) align
-- and enabled = true; the evaluator then routes a notification to the
-- target user (NOTIFY_USER) or to every user in the org whose role
-- assignments include the target role (NOTIFY_ROLE).
CREATE TABLE workflow_rules (
    id               uuid PRIMARY KEY,
    organization_id  uuid NOT NULL REFERENCES organizations(uuid),
    name             text NOT NULL,
    description      text,
    event_kind       text NOT NULL,
    action_type      text NOT NULL,
    action_target    text NOT NULL,
    enabled          boolean NOT NULL DEFAULT true,
    created_at       timestamp NOT NULL DEFAULT current_timestamp,
    updated_at       timestamp,
    CONSTRAINT workflow_rules_action_type_chk
        CHECK (action_type IN ('NOTIFY_USER','NOTIFY_ROLE'))
);
CREATE INDEX workflow_rules_lookup_idx
    ON workflow_rules(organization_id, event_kind)
    WHERE enabled = true;
