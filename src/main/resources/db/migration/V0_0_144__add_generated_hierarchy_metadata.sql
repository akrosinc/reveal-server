CREATE TABLE IF NOT EXISTS generated_hierarchy_metadata
(
    id SERIAL,
    location_identifier character varying NOT NULL,
    tag  character varying,
    value float,
    generated_hierarchy_id  int,
    PRIMARY KEY (id),
    FOREIGN KEY (generated_hierarchy_id)
    REFERENCES generated_hierarchy (id)
);
