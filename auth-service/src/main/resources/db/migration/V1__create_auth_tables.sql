-- V1__create_auth_tables.sql

CREATE TABLE IF NOT EXISTS auth_users (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    username                VARCHAR(50)     NOT NULL UNIQUE,
    email                   VARCHAR(255)    NOT NULL UNIQUE,
    password_hash           VARCHAR(255)    NOT NULL,
    role                    VARCHAR(20)     NOT NULL DEFAULT 'USER',
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    is_email_verified       BOOLEAN         NOT NULL DEFAULT FALSE,
    -- Brute-force protection
    failed_login_attempts   INTEGER         NOT NULL DEFAULT 0,
    locked_until            TIMESTAMP WITH TIME ZONE,
    -- Password reset
    reset_token             VARCHAR(255),
    reset_token_expiry      TIMESTAMP WITH TIME ZONE,
    -- Timestamps
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_login_at           TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_auth_users_email    ON auth_users(email);
CREATE INDEX idx_auth_users_username ON auth_users(username);
CREATE INDEX idx_auth_users_role     ON auth_users(role);

-- Refresh tokens (stored in DB for revocation support; short-lived JWTs are stateless)
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_user_id    UUID            NOT NULL REFERENCES auth_users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255)    NOT NULL UNIQUE,   -- SHA-256 of the raw token
    device_info     VARCHAR(255),
    ip_address      VARCHAR(50),
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked         BOOLEAN         NOT NULL DEFAULT FALSE,
    revoked_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens(auth_user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_auth_users_updated_at
    BEFORE UPDATE ON auth_users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
