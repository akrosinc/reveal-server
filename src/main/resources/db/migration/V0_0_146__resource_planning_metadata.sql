CREATE TABLE IF NOT EXISTS resource_planning_metadata
(
    identifier          SERIAL,
    location_identifier character varying,
    tag                 character varying,
    value               DOUBLE PRECISION,
    resource_plan       character varying,
    PRIMARY KEY (identifier)
);