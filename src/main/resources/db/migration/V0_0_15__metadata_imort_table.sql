CREATE TABLE IF NOT EXISTS metadata_import
(
    identifier        UUID                     NOT NULL,
    filename          VARCHAR(255)             NOT NULL,
    uploaded_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    uploaded_by       VARCHAR(255)             NOT NULL,
    status            VARCHAR(255),
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS metadata_import_aud
(
    identifier        UUID                     NOT NULL,
    REV               INT                      NOT NULL,
    REVTYPE           INTEGER                  NULL,
    filename          VARCHAR(255)             NOT NULL,
    uploaded_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    uploaded_by       VARCHAR(255)             NOT NULL,
    status            VARCHAR(255),
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);

ALTER TABLE location_metadata
ADD COLUMN metaImport_identifier UUID;