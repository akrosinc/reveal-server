CREATE TABLE IF NOT EXISTS location_stats
(
    identifier          UUID                     NOT NULL,
    dataset_id          VARCHAR(255)             NOT NULL,
    location_identifier UUID                     NOT NULL,
    item_count          BIGINT,
    min                 DOUBLE PRECISION,
    max                 DOUBLE PRECISION,
    sum                 DOUBLE PRECISION,
    mean                DOUBLE PRECISION,
    metadata            JSONB,
    entity_status       VARCHAR(36)              NOT NULL,
    created_by          VARCHAR(36)              NOT NULL,
    created_datetime    TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by         VARCHAR(36)              NOT NULL,
    modified_datetime   TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier),
    FOREIGN KEY (location_identifier) REFERENCES location (identifier)
);

CREATE TABLE IF NOT EXISTS location_stats_aud
(
    identifier          UUID                     NOT NULL,
    rev                 INTEGER                  NOT NULL,
    revtype             SMALLINT,
    dataset_id          VARCHAR(255),
    location_identifier UUID,
    item_count          BIGINT,
    min                 DOUBLE PRECISION,
    max                 DOUBLE PRECISION,
    sum                 DOUBLE PRECISION,
    mean                DOUBLE PRECISION,
    metadata            JSONB,
    entity_status       VARCHAR(36),
    created_by          VARCHAR(36),
    created_datetime    TIMESTAMP WITH TIME ZONE,
    modified_by         VARCHAR(36),
    modified_datetime   TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (identifier, rev)
);

ALTER TABLE IF EXISTS location_stats_aud
    DROP CONSTRAINT IF EXISTS fk_location_stats_aud_rev;

ALTER TABLE IF EXISTS location_stats_aud
    ADD CONSTRAINT fk_location_stats_aud_rev
        FOREIGN KEY (rev)
            REFERENCES revinfo (rev);

CREATE INDEX IF NOT EXISTS idx_location_stats_dataset_id
    ON location_stats(dataset_id);

CREATE INDEX IF NOT EXISTS idx_location_stats_location_identifier
    ON location_stats(location_identifier);

CREATE INDEX IF NOT EXISTS idx_location_stats_dataset_loc
    ON location_stats(dataset_id, location_identifier);
