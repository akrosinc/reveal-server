CREATE SEQUENCE IF NOT EXISTS import_aggregate_numeric_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS import_aggregate_string_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

CREATE TABLE IF NOT EXISTS import_aggregation_numeric
(
    id              SERIAL,
    name            character varying COLLATE pg_catalog."default",
    plan_identifier character varying COLLATE pg_catalog."default",
    event_type      character varying COLLATE pg_catalog."default",
    field_code      character varying COLLATE pg_catalog."default",
    val             double precision,
    ancestor        character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT import_aggregation_numeric_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS import_aggregation_string
(
    id              SERIAL,
    name            character varying COLLATE pg_catalog."default",
    plan_identifier character varying COLLATE pg_catalog."default",
    event_type      character varying COLLATE pg_catalog."default",
    field_code      character varying COLLATE pg_catalog."default",
    val             character varying COLLATE pg_catalog."default",
    ancestor        character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT import_aggregation_string_pkey PRIMARY KEY (id)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS import_aggregate_numeric
AS
SELECT nextval('import_aggregate_numeric_seq'::regclass)    AS id,
       ean2.name,
       ean2.ancestor::character varying                     AS locationidentifier,
       ean2.plan_identifier::character varying              AS planidentifier,
       ean2.event_type                                      AS eventtype,
       ean2.field_code                                      as fieldCode,
       sum(ean2.val::double precision)                      AS sum,
       avg(ean2.val::double precision)                      AS avg,
       percentile_cont(0.5::double precision)
       WITHIN GROUP (ORDER BY (ean2.val::double precision)) AS median
FROM import_aggregation_numeric ean2
GROUP BY ean2.name, ean2.ancestor, ean2.plan_identifier, ean2.event_type, ean2.field_code
WITH DATA;

CREATE INDEX IF NOT EXISTS import_aggregate_numeric_location_idx
    ON import_aggregate_numeric USING btree
        (locationidentifier);

-- View: public.event_aggregate_string_count

-- DROP MATERIALIZED VIEW IF EXISTS public.event_aggregate_string_count;

CREATE MATERIALIZED VIEW IF NOT EXISTS import_aggregate_string_count
AS
SELECT nextval('import_aggregate_string_seq'::regclass) AS id,
       ean2.name,
       ean2.ancestor::character varying                 AS locationidentifier,
       ean2.plan_identifier::character varying          AS planidentifier,
       ean2.event_type                                  AS eventtype,
       ean2.field_code                                  as fieldCode,
       ean2.val::character varying                      AS fieldval,
       count(*)                                         AS count
FROM import_aggregation_string ean2
GROUP BY ean2.name, ean2.ancestor, ean2.plan_identifier, ean2.event_type, ean2.field_code, ean2.val
WITH DATA;
