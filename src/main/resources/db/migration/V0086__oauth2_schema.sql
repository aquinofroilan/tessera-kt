CREATE TABLE oauth2_clients (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    client_id VARCHAR(100) NOT NULL UNIQUE,
    client_secret_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    redirect_uris JSONB NOT NULL,
    allowed_scopes JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE oauth2_codes (
    code VARCHAR(100) PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL REFERENCES oauth2_clients(client_id),
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    user_id UUID NOT NULL REFERENCES users(uuid),
    scopes JSONB NOT NULL,
    redirect_uri VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);
