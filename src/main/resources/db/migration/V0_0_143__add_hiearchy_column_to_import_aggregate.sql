ALTER TABLE IF EXISTS import_aggregation_numeric
    ADD COLUMN IF NOT EXISTS hierarchy_identifier character varying;

ALTER TABLE IF EXISTS import_aggregation_string
    ADD COLUMN IF NOT EXISTS hierarchy_identifier character varying;

CREATE TABLE IF NOT EXISTS generated_hierarchy
(
    id SERIAL,
    name character varying NOT NULL,
    entity_query jsonb NOT NULL,
    node_order character varying[] not null,
    PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS generated_location_relationship
(
    id SERIAL,
    location_identifier character varying NOT NULL,
    parent_identifier character varying,
    generated_hierarchy_id int NOT NULL,
    ancestry character varying[],
    PRIMARY KEY (id),
    FOREIGN KEY (generated_hierarchy_id)
        REFERENCES generated_hierarchy (id)
);

