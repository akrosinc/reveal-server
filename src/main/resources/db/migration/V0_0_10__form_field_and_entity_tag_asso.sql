CREATE TABLE form_field
(
    identifier uuid NOT NULL,
    name character varying NOT NULL,
    display character varying,
    form_title character varying not null,
    data_type character varying NOT NULL DEFAULT 'integer',
    entity_status               VARCHAR(36)              NOT NULL,
    created_by                  VARCHAR(36)              NOT NULL,
    created_datetime            TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by                 VARCHAR(36)              NOT NULL,
    modified_datetime           TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier),
    unique (name,form_title)
);

CREATE TABLE form_field_aud
(
    identifier uuid NOT NULL,
    REV                        INT                      NOT NULL,
    REVTYPE                    INTEGER                  NULL,
    name character varying NOT NULL,
    display character varying,
    form_title character varying not null,
    data_type character varying NOT NULL DEFAULT 'integer',
    entity_status               VARCHAR(36)              NOT NULL,
    created_by                  VARCHAR(36)              NOT NULL,
    created_datetime            TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by                 VARCHAR(36)              NOT NULL,
    modified_datetime           TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);

CREATE TABLE form_field_entity_tag
(
    form_field_identifier uuid NOT NULL,
    entity_tag_identifier uuid NOT NULL,
    PRIMARY KEY (form_field_identifier, entity_tag_identifier),
    FOREIGN KEY (form_field_identifier) REFERENCES form_field (identifier),
    FOREIGN KEY (entity_tag_identifier) REFERENCES entity_tag (identifier)
);

CREATE TABLE form_field_entity_tag_aud
(
    rev               integer NOT NULL,
    revtype           integer,
    form_field_identifier uuid NOT NULL,
    entity_tag_identifier uuid NOT NULL,
    PRIMARY KEY (form_field_identifier, entity_tag_identifier, rev)
);

ALTER TABLE IF EXISTS entity_tag
    ADD COLUMN generated boolean NOT NULL DEFAULT false;

ALTER TABLE IF EXISTS entity_tag
    ADD COLUMN generation_formula character varying;

ALTER TABLE IF EXISTS entity_tag
    ADD COLUMN referenced_fields character varying[];

ALTER TABLE IF EXISTS entity_tag
    ADD COLUMN aggregation_method character varying[];

ALTER TABLE IF EXISTS entity_tag
    ADD COLUMN scope character varying NOT NULL DEFAULT 'Plan';

ALTER TABLE IF EXISTS entity_tag_aud
    ADD COLUMN generated boolean NOT NULL DEFAULT false;

ALTER TABLE IF EXISTS entity_tag_aud
    ADD COLUMN generation_formula character varying;

ALTER TABLE IF EXISTS entity_tag_aud
    ADD COLUMN referenced_fields character varying[];

ALTER TABLE IF EXISTS entity_tag_aud
    ADD COLUMN aggregation_method character varying[];

ALTER TABLE IF EXISTS entity_tag_aud
    ADD COLUMN scope character varying NOT NULL DEFAULT 'Plan';

ALTER TABLE IF EXISTS entity_tag
    ADD COLUMN result_expression character varying;

ALTER TABLE IF EXISTS entity_tag_aud
    ADD COLUMN result_expression character varying;

ALTER TABLE IF EXISTS entity_tag
    ADD COLUMN is_result_literal boolean NOT NULL DEFAULT false;

ALTER TABLE IF EXISTS entity_tag_aud
    ADD COLUMN is_result_literal boolean NOT NULL DEFAULT false;

ALTER TABLE IF EXISTS entity_tag
    ADD COLUMN add_to_metadata boolean NOT NULL DEFAULT false;

ALTER TABLE IF EXISTS entity_tag_aud
    ADD COLUMN add_to_metadata boolean NOT NULL DEFAULT false;