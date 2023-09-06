CREATE TABLE IF NOT EXISTS complex_tag
(
    id                   SERIAL,
    tag_name        character varying,
    hierarchy_id character varying,
    hierarchy_type      character varying,
    tags   jsonb,
    formula           character varying,
    PRIMARY KEY (id)
    );