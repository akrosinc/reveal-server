
-- View: public.mw_import_aggregate_numeric_by_date
DROP MATERIALIZED VIEW IF EXISTS public.mw_import_aggregate_numeric_by_date;

CREATE MATERIALIZED VIEW IF NOT EXISTS public.mw_import_aggregate_numeric_by_date
TABLESPACE pg_default
AS
SELECT gen_random_uuid() AS id,
       ean2.ancestor                                                                    AS name,
       ean2.ancestor                                                                    AS locationidentifier,
       ean2.hierarchy_identifier                                                        AS hierarchyidentifier,
       ean2.plan_identifier                                                             AS planidentifier,
       ean2.event_type                                                                  AS eventtype,
       ean2.field_code                                                                  AS fieldcode,
       ean2.data_capture_date                                                           AS datacapturedate,
       COALESCE(EXTRACT(YEAR FROM ean2.data_capture_date), 0)                           AS year,
    sum(ean2.val)                                                                    AS sum,
    avg(ean2.val)                                                                    AS avg,
    percentile_cont(0.5::double precision) WITHIN GROUP (ORDER BY ean2.val)          AS median,
    min(ean2.val)                                                                    AS min,
    max(ean2.val)                                                                    AS max,
    count(ean2.val)                                                                  AS count
FROM import_aggregation_numeric ean2
GROUP BY
    ean2.hierarchy_identifier,
    ean2.ancestor,
    ean2.plan_identifier,
    ean2.event_type,
    ean2.field_code,
    ean2.data_capture_date
WITH DATA;


-- UNIQUE INDEX on id
CREATE UNIQUE INDEX mw_import_aggregate_numeric_by_date_id_idx
    ON public.mw_import_aggregate_numeric_by_date USING btree (id)
    TABLESPACE pg_default;

-- INDEX on locationidentifier
CREATE INDEX mw_import_aggregate_numeric_by_date_location_idx
    ON public.mw_import_aggregate_numeric_by_date USING btree
    (locationidentifier COLLATE pg_catalog."default")
    TABLESPACE pg_default;

-- INDEX on year
CREATE INDEX mw_import_aggregate_numeric_by_date_year_idx
    ON public.mw_import_aggregate_numeric_by_date USING btree (year)
    TABLESPACE pg_default;

-- INDEX on fieldcode (tag)
CREATE INDEX mw_import_aggregate_numeric_by_date_fieldcode_idx
    ON public.mw_import_aggregate_numeric_by_date USING btree
    (fieldcode COLLATE pg_catalog."default")
    TABLESPACE pg_default;

-- INDEX on hierarchyidentifier
CREATE INDEX mw_import_aggregate_numeric_by_date_hierarchy_idx
    ON public.mw_import_aggregate_numeric_by_date USING btree
    (hierarchyidentifier COLLATE pg_catalog."default")
    TABLESPACE pg_default;

-- COMPOSITE INDEX on locationidentifier + year + fieldcode + hierarchyidentifier
-- most useful for heatmap queries that filter on all four
CREATE INDEX mw_import_aggregate_numeric_by_date_composite_idx
    ON public.mw_import_aggregate_numeric_by_date USING btree
    (locationidentifier COLLATE pg_catalog."default",
    year,
    fieldcode COLLATE pg_catalog."default",
    hierarchyidentifier COLLATE pg_catalog."default")
    TABLESPACE pg_default;
