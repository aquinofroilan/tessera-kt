-- Per-user delivery preferences for notifications. Default behaviour is
-- "everything on every channel"; rows here are explicit deviations, so an
-- absent row means the channel × kind pair is enabled for that user.
--
-- The PUT endpoint is responsible for deleting rows that flip back to the
-- default (enabled=true), so this table stays small.
CREATE TABLE notification_preferences (
    id               uuid PRIMARY KEY,
    organization_id  uuid NOT NULL REFERENCES organizations(uuid),
    user_id          uuid NOT NULL REFERENCES users(uuid) ON DELETE CASCADE,
    kind             text NOT NULL,
    channel          text NOT NULL,
    enabled          boolean NOT NULL DEFAULT true,
    created_at       timestamp NOT NULL DEFAULT current_timestamp,
    updated_at       timestamp,
    CONSTRAINT notification_preferences_channel_chk
        CHECK (channel IN ('IN_APP','EMAIL')),
    CONSTRAINT notification_preferences_unique
        UNIQUE (organization_id, user_id, kind, channel)
);
CREATE INDEX notification_preferences_lookup_idx
    ON notification_preferences(user_id, organization_id, kind, channel);
