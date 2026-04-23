CREATE TABLE common_schema.refresh_tokens (
                                              id UUID PRIMARY KEY,
                                              user_id UUID NOT NULL,          -- The user this token belongs to
                                              token_hash TEXT NOT NULL UNIQUE, -- Store a HASH of the token, not the raw string
                                              tenant_id UUID,                 -- NULL for Platform users, UUID for Tenant users
                                              expiry_date TIMESTAMP NOT NULL,
                                              is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    -- Auditable fields
                                              sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast lookups during the /refresh call
CREATE INDEX idx_refresh_token_hash ON common_schema.refresh_tokens(token_hash);