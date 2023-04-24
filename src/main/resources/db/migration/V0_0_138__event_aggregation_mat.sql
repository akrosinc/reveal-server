CREATE SEQUENCE IF NOT EXISTS event_aggregate_numeric_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS event_aggregate_string_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

CREATE MATERIALIZED VIEW IF NOT EXISTS public.event_aggregate_numeric
TABLESPACE pg_default
AS
SELECT nextval('event_aggregate_numeric_seq'::regclass) AS id,
       ean2.name,
       ean2.ancestor::character varying AS locationidentifier,
    ean2.plan_identifier::character varying AS planidentifier,
    ean2.event_type AS eventtype,
    ean2.fieldcode,
    sum(ean2.val::double precision) AS sum,
    avg(ean2.val::double precision) AS avg,
    percentile_cont(0.5::double precision) WITHIN GROUP (ORDER BY (ean2.val::double precision)) AS median
FROM event_aggregation_numeric ean2
GROUP BY ean2.name, ean2.ancestor, ean2.plan_identifier, ean2.event_type, ean2.fieldcode
WITH DATA;

CREATE INDEX  IF NOT EXISTS event_aggregate_numeric_location_idx
    ON public.event_aggregate_numeric USING btree
    (locationidentifier COLLATE pg_catalog."default")
    TABLESPACE pg_default;

-- View: public.event_aggregate_string_count

-- DROP MATERIALIZED VIEW IF EXISTS public.event_aggregate_string_count;

CREATE MATERIALIZED VIEW IF NOT EXISTS public.event_aggregate_string_count
TABLESPACE pg_default
AS
SELECT nextval('event_aggregate_string_seq'::regclass) AS id,
       ean2.name,
       ean2.ancestor::character varying AS locationidentifier,
    ean2.plan_identifier::character varying AS planidentifier,
    ean2.event_type AS eventtype,
    ean2.fieldcode,
    ean2.val::character varying AS fieldval,
    count(*) AS count
FROM event_aggregation_string_count ean2
GROUP BY ean2.name, ean2.ancestor, ean2.plan_identifier, ean2.event_type, ean2.fieldcode, ean2.val
WITH DATA;
