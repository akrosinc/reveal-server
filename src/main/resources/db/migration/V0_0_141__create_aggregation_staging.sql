CREATE TABLE IF NOT EXISTS aggregation_staging
(
    id SERIAL,
    location_identifier UUID,
    hierarchy_identifier character varying,
    node_order character varying,
    PRIMARY KEY (id)
);

