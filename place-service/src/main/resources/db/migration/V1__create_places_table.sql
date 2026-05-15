-- V1__create_places_table.sql
-- Enable PostGIS extension (requires superuser or rds_superuser on AWS RDS)
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- for ILIKE trigram index on name/address

CREATE TABLE IF NOT EXISTS places (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id         VARCHAR(255),                   -- Google Places place_id or OSM node id
    source              VARCHAR(30)     NOT NULL DEFAULT 'USER',   -- GOOGLE | OSM | USER
    name                VARCHAR(255)    NOT NULL,
    description         TEXT,
    category            VARCHAR(50)     NOT NULL,
    sub_category        VARCHAR(50),
    address             TEXT,
    city                VARCHAR(100),
    state               VARCHAR(100),
    country             VARCHAR(100),
    postal_code         VARCHAR(20),
    location            GEOMETRY(Point, 4326) NOT NULL,  -- PostGIS point (lng, lat, SRID=WGS84)
    phone_number        VARCHAR(30),
    website             VARCHAR(500),
    google_maps_url     VARCHAR(500),
    opening_hours       JSONB,                          -- {"mon":"09:00-21:00", ...}
    photos              JSONB,                          -- ["url1","url2"]
    amenities           JSONB,                          -- ["wifi","parking",...]
    -- Aggregated safety data (materialised from Rating Service via Kafka)
    safety_score        NUMERIC(3,2)    NOT NULL DEFAULT 0.00,  -- 0.00 – 5.00
    total_ratings       INTEGER         NOT NULL DEFAULT 0,
    -- Moderation
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    is_verified         BOOLEAN         NOT NULL DEFAULT FALSE,
    reported_count      INTEGER         NOT NULL DEFAULT 0,
    -- Ownership
    created_by          UUID            NOT NULL,               -- User.id (not authUserId)
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_external_id_source UNIQUE (external_id, source)
);

-- Spatial index (key for ST_DWithin geo-search)
CREATE INDEX idx_places_location       ON places USING GIST(location);

-- Text search indexes
CREATE INDEX idx_places_name_trgm      ON places USING GIN(name gin_trgm_ops);
CREATE INDEX idx_places_address_trgm   ON places USING GIN(address gin_trgm_ops);
CREATE INDEX idx_places_city           ON places(city);
CREATE INDEX idx_places_country        ON places(country);
CREATE INDEX idx_places_category       ON places(category);
CREATE INDEX idx_places_source         ON places(source);
CREATE INDEX idx_places_is_active      ON places(is_active);
CREATE INDEX idx_places_created_by     ON places(created_by);
CREATE INDEX idx_places_safety_score   ON places(safety_score DESC);

-- updated_at trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_places_updated_at
    BEFORE UPDATE ON places
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
