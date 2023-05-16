CREATE TABLE  IF NOT EXISTS  event_tracker
(
    identifier uuid NOT NULL,
    event_identifier uuid NOT NULL,
    entity_tag_identifier uuid NOT NULL,
    scope character varying,
    date character varying,
    entity_status               VARCHAR(36)              NOT NULL,
    created_by                  VARCHAR(36)              NOT NULL,
    created_datetime            TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by                 VARCHAR(36)              NOT NULL,
    modified_datetime           TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE  IF NOT EXISTS event_tracker_aud
(
    identifier uuid NOT NULL,
    REV                        INT                      NOT NULL,
    REVTYPE                    INTEGER                  NULL,
    event_identifier uuid NOT NULL,
    entity_tag_identifier uuid NOT NULL,
    scope character varying,
    date character varying ,
    entity_status               VARCHAR(36)              NOT NULL,
    created_by                  VARCHAR(36)              NOT NULL,
    created_datetime            TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by                 VARCHAR(36)              NOT NULL,
    modified_datetime           TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);
