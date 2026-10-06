CREATE TABLE IF NOT EXISTS  ingestion_task (
                                               identifier      UUID PRIMARY KEY,
                                               task_identifier VARCHAR(255),
    type            VARCHAR(50),
    stage           VARCHAR(50),
    message         TEXT,
    total_steps     INT NOT NULL DEFAULT 0,
    completed_steps INT NOT NULL DEFAULT 0,
    failed          BOOLEAN NOT NULL DEFAULT FALSE,
    last_updated    TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS map_layer
(
    id                UUID                     NOT NULL,
    name              VARCHAR(255)             NOT NULL,
    layer_identifier  VARCHAR(255)             NOT NULL,
    type              VARCHAR(50)              NOT NULL,
    extent            JSONB,
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
                                    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
                                    PRIMARY KEY (id)
    );

CREATE TABLE IF NOT EXISTS map_layer_aud
(
    id                UUID                     NOT NULL,
    rev               INTEGER                  NOT NULL,
    revtype           SMALLINT,
    name              VARCHAR(255),
    layer_identifier  VARCHAR(255),
    type              VARCHAR(50),
    extent            JSONB,
    active            BOOLEAN,
    entity_status     VARCHAR(36),
    created_by        VARCHAR(36),
    created_datetime  TIMESTAMP WITH TIME ZONE,
                                    modified_by       VARCHAR(36),
    modified_datetime TIMESTAMP WITH TIME ZONE,
                                    PRIMARY KEY (id, rev)
    );

ALTER TABLE IF EXISTS map_layer_aud
DROP CONSTRAINT IF EXISTS fk_map_layer_aud_rev;

ALTER TABLE IF EXISTS map_layer_aud
DROP CONSTRAINT IF EXISTS fk_map_layer_aud_rev;

ALTER TABLE IF EXISTS map_layer_aud
    ADD CONSTRAINT fk_map_layer_aud_rev
    FOREIGN KEY (rev)
    REFERENCES revinfo (rev);

CREATE INDEX IF NOT EXISTS idx_map_layer_identifier
    ON map_layer(layer_identifier);

ALTER TABLE metadata_import
    ADD COLUMN IF NOT EXISTS metadata_import_type VARCHAR(50) NOT NULL DEFAULT 'CSV';


ALTER TABLE IF EXISTS raster_location_zonal_stats
    ADD COLUMN IF NOT EXISTS tag VARCHAR(255);

ALTER TABLE IF EXISTS raster_location_zonal_stats_aud
    ADD COLUMN IF NOT EXISTS tag VARCHAR(255);