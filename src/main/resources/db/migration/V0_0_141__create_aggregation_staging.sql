CREATE TABLE IF NOT EXISTS aggregation_staging
(
    id SERIAL,
    location_identifier UUID,
    hierarchy_identifier UUID,
    node_order character varying,
    PRIMARY KEY (id)
);

