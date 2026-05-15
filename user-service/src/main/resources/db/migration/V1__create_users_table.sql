-- V1__create_users_table.sql
CREATE TABLE IF NOT EXISTS users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_user_id    UUID            NOT NULL UNIQUE,          -- FK to Auth Service user (cross-service ref)
    username        VARCHAR(50)     NOT NULL UNIQUE,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    display_name    VARCHAR(100),
    bio             TEXT,
    avatar_url      VARCHAR(500),
    phone_number    VARCHAR(20),
    gender          VARCHAR(20),
    date_of_birth   DATE,
    city            VARCHAR(100),
    country         VARCHAR(100),
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    role            VARCHAR(20)     NOT NULL DEFAULT 'USER',
    is_anonymous    BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    is_verified     BOOLEAN         NOT NULL DEFAULT FALSE,
    total_reviews   INTEGER         NOT NULL DEFAULT 0,
    helpful_votes   INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_seen_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_users_auth_user_id  ON users(auth_user_id);
CREATE INDEX idx_users_username      ON users(username);
CREATE INDEX idx_users_email         ON users(email);
CREATE INDEX idx_users_role          ON users(role);
CREATE INDEX idx_users_city_country  ON users(city, country);
CREATE INDEX idx_users_is_active     ON users(is_active);

-- Trigger: auto-update updated_at on row change
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
