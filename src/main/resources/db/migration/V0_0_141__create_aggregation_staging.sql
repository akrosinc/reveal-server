CREATE TABLE IF NOT EXISTS aggregation_staging
(
    id SERIAL,
    location_identifier UUID,
    hierarchy_identifier UUID,
    PRIMARY KEY (id)
);