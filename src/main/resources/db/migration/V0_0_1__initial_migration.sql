CREATE EXTENSION IF NOT EXISTS "unaccent" WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS postgis_raster WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;

CREATE TABLE revinfo
(
    rev       INTEGER PRIMARY KEY DEFAULT 1,
    revtstmp  BIGINT    NOT NULL,
    timestamp TIMESTAMP NOT NULL  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task
(
    identifier             VARCHAR(36)              NOT NULL,
    entity_status          VARCHAR(36)              NOT NULL,
    created_by             VARCHAR(36)              NOT NULL,
    created_datetime       TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by            VARCHAR(36)              NOT NULL,
    modified_datetime      TIMESTAMP WITH TIME ZONE NOT NULL,
    plan_identifier        VARCHAR(36)              NOT NULL,
    focus                  VARCHAR(36)              NOT NULL,
    code                   VARCHAR(36)              NOT NULL,
    status                 VARCHAR(36)              NOT NULL,
    priority               VARCHAR(36)              NOT NULL,
    authored_on            TIMESTAMP WITH TIME ZONE NOT NULL,
    description            VARCHAR(255)             NOT NULL,
    last_modified          TIMESTAMP WITH TIME ZONE NOT NULL,
    business_status        VARCHAR(36)              NOT NULL,
    execution_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    execution_period_end   TIMESTAMP WITH TIME ZONE NOT NULL,
    group_identifier       VARCHAR(36)              NOT NULL,
    instantiates_uri       VARCHAR(36)              NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE INDEX IF NOT EXISTS task_idx ON task (identifier);
CREATE INDEX IF NOT EXISTS task_plan_identifier_idx ON task (plan_identifier);
CREATE INDEX IF NOT EXISTS task_status_idx ON task (status);
CREATE INDEX IF NOT EXISTS task_business_status_idx ON task (business_status);

CREATE TABLE IF NOT EXISTS task_aud
(
    identifier             VARCHAR(36)              NOT NULL,
    REV                    INT                      NOT NULL,
    REVTYPE                INTEGER                  NULL,
    entity_status          VARCHAR(36)              NOT NULL,
    created_by             VARCHAR(36)              NOT NULL,
    created_datetime       TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by            VARCHAR(36)              NOT NULL,
    modified_datetime      TIMESTAMP WITH TIME ZONE NOT NULL,
    plan_identifier        VARCHAR(36)              NOT NULL,
    focus                  VARCHAR(36)              NOT NULL,
    code                   VARCHAR(36)              NOT NULL,
    status                 VARCHAR(36)              NOT NULL,
    priority               VARCHAR(36)              NOT NULL,
    authored_on            TIMESTAMP WITH TIME ZONE NOT NULL,
    description            VARCHAR(255)             NOT NULL,
    last_modified          TIMESTAMP WITH TIME ZONE NOT NULL,
    business_status        VARCHAR(36)              NOT NULL,
    execution_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    execution_period_end   TIMESTAMP WITH TIME ZONE NOT NULL,
    group_identifier       VARCHAR(36)              NOT NULL,
    instantiates_uri       VARCHAR(36)              NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS plan
(
    identifier             UUID UNIQUE              NOT NULL,
    name                   VARCHAR(36),
    title                  VARCHAR(36),
    status                 VARCHAR(36),
    date                   TIMESTAMP,
    effective_period_start DATE,
    effective_period_end   DATE,
    intervention_type      VARCHAR(36),
    entity_status          VARCHAR(36)              NOT NULL,
    created_by             VARCHAR(36)              NOT NULL,
    created_datetime       TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by            VARCHAR(36)              NOT NULL,
    modified_datetime      TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS plan_aud
(
    identifier             UUID                     NOT NULL,
    REV                    INT                      NOT NULL,
    REVTYPE                INTEGER                  NULL,
    name                   VARCHAR(36),
    title                  VARCHAR(36),
    status                 VARCHAR(36),
    date                   TIMESTAMP,
    effective_period_start DATE,
    effective_period_end   DATE,
    intervention_type      VARCHAR(36),
    entity_status          VARCHAR(36)              NOT NULL,
    created_by             VARCHAR(36)              NOT NULL,
    created_datetime       TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by            VARCHAR(36)              NOT NULL,
    modified_datetime      TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);

CREATE TABLE IF NOT EXISTS geographic_level
(
    identifier        UUID UNIQUE              NOT NULL,
    name              VARCHAR(255)             NOT NULL,
    title             VARCHAR(255),
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (name, entity_status),
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS geographic_level_aud
(
    identifier        UUID                     NOT NULL,
    REV               INT                      NOT NULL,
    REVTYPE           INTEGER                  NOT NULL,
    name              VARCHAR(255),
    title             VARCHAR(255),
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);

CREATE TABLE IF NOT EXISTS location_hierarchy
(
    identifier        UUID UNIQUE              NOT NULL,
    node_order        VARCHAR[]                NOT NULL,
    name              VARCHAR(36)              NOT NULL,
    entity_status     VARCHAR(36),
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS location_hierarchy_aud
(
    identifier        UUID                     NOT NULL,
    REV               INT                      NOT NULL,
    REVTYPE           INTEGER                  NULL,
    node_order        VARCHAR[]                NOT NULL,
    name              VARCHAR(36)              NOT NULL,
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);
CREATE TABLE IF NOT EXISTS location_bulk
(
    identifier        UUID                     NOT NULL,
    filename          VARCHAR(255)             NOT NULL,
    uploaded_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    status            VARCHAR(255),
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS location_bulk_aud
(
    identifier        UUID                     NOT NULL,
    REV               INT                      NOT NULL,
    REVTYPE           INTEGER                  NULL,
    filename          VARCHAR(255)             NOT NULL,
    uploaded_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    status            VARCHAR(255),
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);

CREATE TABLE IF NOT EXISTS location
(
    identifier                  UUID UNIQUE              NOT NULL,
    name                        VARCHAR(255)             NOT NULL,
    geometry                    jsonb                    NOT NULL,
    type                        VARCHAR(255)             NOT NULL,
    status                      VARCHAR(255)             NOT NULL,
    external_id                 UUID,
    geographic_level_identifier UUID                     NOT NULL,
    entity_status               VARCHAR(36)              NOT NULL,
    location_bulk_identifier    UUID,
    created_by                  VARCHAR(36)              NOT NULL,
    created_datetime            TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by                 VARCHAR(36)              NOT NULL,
    modified_datetime           TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier),
    FOREIGN KEY (location_bulk_identifier) REFERENCES location_bulk (identifier)

);

CREATE TABLE IF NOT EXISTS location_aud
(
    identifier                  UUID                     NOT NULL,
    REV                         INT                      NOT NULL,
    REVTYPE                     INTEGER                  NULL,
    name                        VARCHAR(255)             NOT NULL,
    geometry                    jsonb                    NOT NULL,
    type                        VARCHAR(255)             NOT NULL,
    status                      VARCHAR(255)             NOT NULL,
    external_id                 UUID,
    geographic_level_identifier UUID                     NOT NULL,
    entity_status               VARCHAR(36)              NOT NULL,
    location_bulk_identifier    UUID,
    created_by                  VARCHAR(36)              NOT NULL,
    created_datetime            TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by                 VARCHAR(36)              NOT NULL,
    modified_datetime           TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);


CREATE TABLE IF NOT EXISTS location_bulk_exception
(
    identifier               UUID                     NOT NULL,
    name                     VARCHAR(255),
    message                  VARCHAR(255),
    location_bulk_identifier UUID                     NOT NULL,
    entity_status            VARCHAR(36)              NOT NULL,
    created_by               VARCHAR(36)              NOT NULL,
    created_datetime         TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by              VARCHAR(36)              NOT NULL,
    modified_datetime        TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier),
    FOREIGN KEY (location_bulk_identifier) REFERENCES location_bulk (identifier)
);

CREATE TABLE IF NOT EXISTS location_bulk_exception_aud
(
    identifier               UUID                     NOT NULL,
    REV                      INT                      NOT NULL,
    REVTYPE                  INTEGER                  NULL,
    name                     VARCHAR(255),
    message                  VARCHAR(255),
    location_bulk_identifier UUID                     NOT NULL,
    entity_status            VARCHAR(36)              NOT NULL,
    created_by               VARCHAR(36)              NOT NULL,
    created_datetime         TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by              VARCHAR(36)              NOT NULL,
    modified_datetime        TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);

CREATE TABLE IF NOT EXISTS location_relationship
(
    identifier                    UUID UNIQUE              NOT NULL,
    location_hierarchy_identifier UUID                     NOT NULL,
    location_identifier           UUID                     NOT NULL,
    parent_identifier             UUID,
    ancestry                      UUID[]                   NOT NULL,
    entity_status                 VARCHAR(36)              NOT NULL,
    created_by                    VARCHAR(36),
    created_datetime              TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by                   VARCHAR(36),
    modified_datetime             TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS location_relationship_aud
(
    identifier                    UUID                     NOT NULL,
    REV                           INT                      NOT NULL,
    REVTYPE                       INTEGER                  NULL,
    location_hierarchy_identifier UUID                     NOT NULL,
    location_identifier           UUID                     NOT NULL,
    parent_identifier             UUID,
    ancestry                      UUID[]                   NOT NULL,
    entity_status                 VARCHAR(36)              NOT NULL,
    created_by                    VARCHAR(36),
    created_datetime              TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by                   VARCHAR(36),
    modified_datetime             TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);

CREATE TABLE IF NOT EXISTS raster_store
(
    id                BIGINT                   NOT NULL,
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    rid               serial primary key,
    rast              raster,
    file_name         VARCHAR(36)
);

CREATE TABLE IF NOT EXISTS organization
(
    identifier             UUID                     NOT NULL,
    active                 BOOLEAN,
    name                   VARCHAR(255)             NOT NULL,
    type                   VARCHAR(36)              NOT NULL,
    organization_parent_id UUID,
    entity_status          VARCHAR(36)              NOT NULL,
    created_by             VARCHAR(36)              NOT NULL,
    created_datetime       TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by            VARCHAR(36)              NOT NULL,
    modified_datetime      TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS organization_aud
(
    identifier             UUID                     NOT NULL,
    REV                    INT                      NOT NULL,
    REVTYPE                INTEGER                  NULL,
    active                 BOOLEAN,
    name                   VARCHAR(255)             NOT NULL,
    type                   VARCHAR(36)              NOT NULL,
    organization_parent_id UUID,
    entity_status          VARCHAR(36)              NOT NULL,
    created_by             VARCHAR(36)              NOT NULL,
    created_datetime       TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by            VARCHAR(36)              NOT NULL,
    modified_datetime      TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);

CREATE TABLE IF NOT EXISTS user_bulk
(
    identifier        UUID                     NOT NULL,
    filename          VARCHAR(255)             NOT NULL,
    uploaded_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    status            VARCHAR(255),
    uploaded_by       VARCHAR(255)             NOT NULL,
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS user_bulk_aud
(
    identifier        UUID                     NOT NULL,
    REV               INT                      NOT NULL,
    REVTYPE           INTEGER                  NULL,
    filename          VARCHAR(255)             NOT NULL,
    uploaded_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    status            VARCHAR(255),
    uploaded_by       VARCHAR(255)             NOT NULL,
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);

CREATE TABLE IF NOT EXISTS users
(
    identifier           UUID                     NOT NULL,
    sid                  UUID,
    username             VARCHAR(255)             NOT NULL,
    first_name           VARCHAR(255)             NOT NULL,
    last_name            VARCHAR(255)             NOT NULL,
    email                VARCHAR(255),
    api_response         VARCHAR(255),
    user_bulk_identifier UUID,
    entity_status        VARCHAR(36)              NOT NULL,
    created_by           VARCHAR(36)              NOT NULL,
    created_datetime     TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by          VARCHAR(36)              NOT NULL,
    modified_datetime    TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier),
    FOREIGN KEY (user_bulk_identifier) REFERENCES user_bulk (identifier)
);

CREATE TABLE IF NOT EXISTS users_aud
(
    identifier           UUID                     NOT NULL,
    REV                  INT                      NOT NULL,
    REVTYPE              INTEGER                  NULL,
    sid                  UUID,
    username             VARCHAR(255)             NOT NULL,
    first_name           VARCHAR(255)             NOT NULL,
    last_name            VARCHAR(255)             NOT NULL,
    email                VARCHAR(255),
    api_response         VARCHAR(255),
    user_bulk_identifier UUID,
    entity_status        VARCHAR(36)              NOT NULL,
    created_by           VARCHAR(36)              NOT NULL,
    created_datetime     TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by          VARCHAR(36)              NOT NULL,
    modified_datetime    TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);

CREATE TABLE IF NOT EXISTS "user_organization"
(
    user_identifier         UUID NOT NULL,
    organization_identifier UUID NOT NULL,
    PRIMARY KEY (user_identifier, organization_identifier),
    FOREIGN KEY (user_identifier) REFERENCES users (identifier),
    FOREIGN KEY (organization_identifier) REFERENCES organization (identifier)
);

CREATE TABLE IF NOT EXISTS "user_organization_aud"
(
    user_identifier         UUID    NOT NULL,
    organization_identifier UUID    NOT NULL,
    REV                     INT     NOT NULL,
    REVTYPE                 INTEGER NULL,
    PRIMARY KEY (user_identifier, organization_identifier, REV),
    FOREIGN KEY (user_identifier) REFERENCES users (identifier),
    FOREIGN KEY (organization_identifier) REFERENCES organization (identifier)
);

CREATE TABLE IF NOT EXISTS user_security_groups
(
    identifier     UUID NOT NULL,
    security_group VARCHAR(255),
    FOREIGN KEY (identifier) REFERENCES users (identifier),
    PRIMARY KEY (identifier, security_group)
);

CREATE TABLE IF NOT EXISTS user_security_groups_aud
(
    identifier     UUID    NOT NULL,
    security_group VARCHAR(255),
    REV            INT     NOT NULL,
    REVTYPE        INTEGER NULL,
    FOREIGN KEY (identifier) REFERENCES users (identifier),
    PRIMARY KEY (identifier, security_group, REV)
);

CREATE TABLE IF NOT EXISTS user_bulk_exception
(
    identifier           UUID                     NOT NULL,
    username             VARCHAR(255),
    message              VARCHAR(255),
    user_bulk_identifier UUID                     NOT NULL,
    entity_status        VARCHAR(36)              NOT NULL,
    created_by           VARCHAR(36)              NOT NULL,
    created_datetime     TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by          VARCHAR(36)              NOT NULL,
    modified_datetime    TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier),
    FOREIGN KEY (user_bulk_identifier) REFERENCES user_bulk (identifier)
);

CREATE TABLE IF NOT EXISTS user_bulk_exception_aud
(
    identifier           UUID                     NOT NULL,
    REV                  INT                      NOT NULL,
    REVTYPE              INTEGER                  NULL,
    username             VARCHAR(255),
    message              VARCHAR(255),
    user_bulk_identifier UUID                     NOT NULL,
    entity_status        VARCHAR(36)              NOT NULL,
    created_by           VARCHAR(36)              NOT NULL,
    created_datetime     TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by          VARCHAR(36)              NOT NULL,
    modified_datetime    TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);

CREATE SEQUENCE IF NOT EXISTS hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    MAXVALUE 9223372036854775807;

CREATE INDEX IF NOT EXISTS raster_store_idx ON raster_store (id);
CREATE INDEX raster_store_rast_st_convexhull_idx ON raster_store USING gist (public.ST_ConvexHull(rast));

CREATE TABLE IF NOT EXISTS person
(
    identifier        uuid                     NOT NULL,
    active            boolean           DEFAULT true,
    name_use          character varying        NOT NULL,
    name_text         character varying        NOT NULL,
    name_family       character varying        NOT NULL,
    name_given        character varying        NOT NULL,
    name_prefix       character varying(30)    NOT NULL,
    name_suffix       character varying DEFAULT ''::character varying,
    gender            character varying(30)    NOT NULL,
    birth_date        date                     NOT NULL,
    entity_status     character varying(36)    NOT NULL,
    created_by        character varying(36)    NOT NULL,
    created_datetime  timestamp with time zone NOT NULL,
    modified_by       character varying(36)    NOT NULL,
    modified_datetime timestamp with time zone NOT NULL,
    CONSTRAINT person_pkey PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS person_aud
(
    identifier        uuid                     NOT NULL,
    rev               integer                  NOT NULL,
    revtype           integer,
    active            boolean           DEFAULT true,
    name_use          character varying        NOT NULL,
    name_text         character varying        NOT NULL,
    name_family       character varying        NOT NULL,
    name_given        character varying        NOT NULL,
    name_prefix       character varying(30)    NOT NULL,
    name_suffix       character varying DEFAULT ''::character varying,
    gender            character varying(30)    NOT NULL,
    birth_date        date                     NOT NULL,
    created_by        character varying(36)    NOT NULL,
    created_datetime  timestamp with time zone NOT NULL,
    modified_by       character varying(36)    NOT NULL,
    modified_datetime timestamp with time zone NOT NULL,
    entity_status     character varying(36)    NOT NULL,
    CONSTRAINT person_aud_pkey PRIMARY KEY (identifier, rev)
);

CREATE TABLE IF NOT EXISTS "group"
(
    identifier          uuid                     NOT NULL,
    name                character varying(255)   NOT NULL,
    type                character varying(255)   NOT NULL,
    location_identifier uuid,
    entity_status       character varying(36)    NOT NULL,
    created_by          character varying(36)    NOT NULL,
    created_datetime    timestamp with time zone NOT NULL,
    modified_by         character varying(36)    NOT NULL,
    modified_datetime   timestamp with time zone NOT NULL,
    CONSTRAINT group_pkey PRIMARY KEY (identifier),
    CONSTRAINT group_location_identifier_fkey FOREIGN KEY (location_identifier)
        REFERENCES location (identifier) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS group_aud
(
    identifier          uuid                     NOT NULL,
    name                character varying(255)   NOT NULL,
    type                character varying(255)   NOT NULL,
    rev                 integer                  NOT NULL,
    revtype             integer,
    location_identifier uuid,
    created_by          character varying(36)    NOT NULL,
    created_datetime    timestamp with time zone NOT NULL,
    modified_by         character varying(36)    NOT NULL,
    modified_datetime   timestamp with time zone NOT NULL,
    entity_status       character varying(36)    NOT NULL,
    CONSTRAINT group_aud_pkey PRIMARY KEY (identifier, rev),
    CONSTRAINT group_aud_location_identifier_fkey FOREIGN KEY (location_identifier)
        REFERENCES location (identifier) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS person_group
(
    person_identifier uuid NOT NULL,
    group_identifier  uuid NOT NULL,
    CONSTRAINT person_group_pkey PRIMARY KEY (person_identifier, group_identifier),
    CONSTRAINT person_group_group_identifier_fkey FOREIGN KEY (group_identifier)
        REFERENCES "group" (identifier) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT person_group_person_identifier_fkey FOREIGN KEY (person_identifier)
        REFERENCES person (identifier) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS person_group_aud
(
    rev               integer NOT NULL,
    revtype           integer,
    person_identifier uuid    NOT NULL,
    group_identifier  uuid    NOT NULL,
    CONSTRAINT person_group_aud_pkey PRIMARY KEY (person_identifier, group_identifier, rev),
    CONSTRAINT person_group_aud_group_identifier_fkey FOREIGN KEY (group_identifier)
        REFERENCES "group" (identifier) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT person_group_aud_person_identifier_fkey FOREIGN KEY (person_identifier)
        REFERENCES person (identifier) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);


