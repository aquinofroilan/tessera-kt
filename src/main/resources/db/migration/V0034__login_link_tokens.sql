-- Auth: passwordless magic-link login tokens.
-- Single-use, short-lived tokens that authenticate an existing user via a
-- link sent to their email. Distinct from password_reset_tokens so the two
-- flows cannot be confused: a password-reset URL cannot mint a session,
-- and a magic-link URL cannot change a password.

CREATE TABLE login_link_tokens (
    id          uuid PRIMARY KEY,
    token_hash  text NOT NULL,
    user_id     uuid NOT NULL REFERENCES users(uuid) ON DELETE CASCADE,
    expiry_at   timestamp NOT NULL,
    consumed_at timestamp,
    ip_address  text,
    user_agent  text,
    created_at  timestamp NOT NULL DEFAULT current_timestamp,
    CONSTRAINT login_link_tokens_unique_hash UNIQUE (token_hash)
);
CREATE INDEX login_link_tokens_user_idx ON login_link_tokens(user_id);
CREATE INDEX login_link_tokens_open_idx ON login_link_tokens(expiry_at) WHERE consumed_at IS NULL;
