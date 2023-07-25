CREATE TABLE IF NOT EXISTS resource_aggregation_numeric
(
    id                   SERIAL,
    location_name        character varying,
    hierarchy_identifier character varying,
    plan_identifier      character varying,
    resource_plan_name   character varying,
    field_code           character varying,
    val                  double precision,
    ancestor             character varying NOT NULL,
    PRIMARY KEY (id)
    );

CREATE SEQUENCE IF NOT EXISTS resource_aggregate_numeric_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

CREATE MATERIALIZED VIEW IF NOT EXISTS resource_aggregate_numeric
AS
SELECT nextval('resource_aggregate_numeric_seq'::regclass)    AS id,
       ean2.ancestor                                        as name,
       ean2.ancestor::character varying                     AS locationidentifier,
       ean2.hierarchy_identifier                            AS hierarchyidentifier,
       ean2.plan_identifier::character varying              AS planidentifier,
       ean2.resource_plan_name                              AS eventtype,
       ean2.field_code                                      as fieldCode,
       sum(ean2.val::double precision)                      AS sum,
       avg(ean2.val::double precision)                      AS avg,
       percentile_cont(0.5::double precision)
       WITHIN GROUP (ORDER BY (ean2.val::double precision)) AS median,
       min(ean2.val::double precision)                      as min,
       max(ean2.val::double precision)                      as max
FROM resource_aggregation_numeric ean2
GROUP BY ean2.hierarchy_identifier, ean2.ancestor, ean2.ancestor, ean2.plan_identifier,
    ean2.resource_plan_name, ean2.field_code
WITH DATA;