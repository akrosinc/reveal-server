DROP TABLE IF EXISTS form_field_entity_tag;

DROP TABLE IF EXISTS entity_tag;

CREATE TABLE IF NOT EXISTS entity_tag
(
    identifier         uuid              NOT NULL,
    tag                character varying NOT NULL,
    value_type         character varying,
    definition         character varying,
    simulation_display boolean default false,
    is_aggregate       boolean DEFAULT false,
    aggregation_method character varying[],
    PRIMARY KEY (identifier)
);