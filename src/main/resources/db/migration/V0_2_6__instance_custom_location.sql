-- Instance-scoped custom locations (sites drawn under a shared Health Facility, and points inside a site).
-- The shared tables location and location_relationship are NOT changed.
-- Every statement is idempotent so the script can be re-run safely.

-- INSTANCE CUSTOM LOCATION
CREATE TABLE IF NOT EXISTS instance_custom_location
(
    identifier                  UUID                     NOT NULL DEFAULT uuid_generate_v4(),
    instance_id                 UUID                     NOT NULL,
    name                        VARCHAR(255)             NOT NULL,
    status                      VARCHAR(255)             NOT NULL,
    geographic_level_identifier UUID,
    parent_location_identifier  UUID,
    parent_identifier           UUID,
    geometry                    geometry(Geometry, 4326) NOT NULL,
    metadata                    jsonb,
    entity_status               VARCHAR(36)              NOT NULL,
    created_by                  VARCHAR(36)              NOT NULL,
    created_datetime            TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by                 VARCHAR(36)              NOT NULL,
    modified_datetime           TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

ALTER TABLE IF EXISTS  instance_custom_location
    DROP CONSTRAINT IF EXISTS fk_icl_instance;
ALTER TABLE IF EXISTS  instance_custom_location
    ADD CONSTRAINT fk_icl_instance
        FOREIGN KEY (instance_id) REFERENCES instance (identifier) ON DELETE CASCADE;

ALTER TABLE IF EXISTS  instance_custom_location
    DROP CONSTRAINT IF EXISTS fk_icl_geographic_level;
ALTER TABLE IF EXISTS  instance_custom_location
    ADD CONSTRAINT fk_icl_geographic_level
        FOREIGN KEY (geographic_level_identifier) REFERENCES geographic_level (identifier);

ALTER TABLE IF EXISTS  instance_custom_location
    DROP CONSTRAINT IF EXISTS fk_icl_parent_location;
ALTER TABLE IF EXISTS  instance_custom_location
    ADD CONSTRAINT fk_icl_parent_location
        FOREIGN KEY (parent_location_identifier) REFERENCES location (identifier);

ALTER TABLE IF EXISTS  instance_custom_location
    DROP CONSTRAINT IF EXISTS fk_icl_parent;
ALTER TABLE IF EXISTS  instance_custom_location
    ADD CONSTRAINT fk_icl_parent
        FOREIGN KEY (parent_identifier) REFERENCES instance_custom_location (identifier) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_icl_geometry
    ON instance_custom_location USING GIST (geometry);
CREATE INDEX IF NOT EXISTS idx_icl_instance_parent_location
    ON instance_custom_location (instance_id, parent_location_identifier);
CREATE INDEX IF NOT EXISTS idx_icl_instance_parent
    ON instance_custom_location (instance_id, parent_identifier);

-- geometry is not audited (see InstanceCustomLocation#geometry)
CREATE TABLE IF NOT EXISTS instance_custom_location_aud
(
    identifier                  UUID                     NOT NULL,
    rev                         INTEGER                  NOT NULL,
    revtype                     SMALLINT,
    instance_id                 UUID,
    name                        VARCHAR(255),
    status                      VARCHAR(255),
    geographic_level_identifier UUID,
    parent_location_identifier  UUID,
    parent_identifier           UUID,
    metadata                    jsonb,
    entity_status               VARCHAR(36),
    created_by                  VARCHAR(36),
    created_datetime            TIMESTAMP WITH TIME ZONE,
    modified_by                 VARCHAR(36),
    modified_datetime           TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (identifier, rev)
);

ALTER TABLE IF EXISTS  instance_custom_location_aud
    DROP CONSTRAINT IF EXISTS fk_icl_aud_rev;
ALTER TABLE IF EXISTS  instance_custom_location_aud
    ADD CONSTRAINT fk_icl_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev);
