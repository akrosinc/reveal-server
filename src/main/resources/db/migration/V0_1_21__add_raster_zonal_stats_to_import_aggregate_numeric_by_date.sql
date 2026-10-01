-- Recreate the materialized view with a UNION against raster_location_zonal_stats
DROP MATERIALIZED VIEW IF EXISTS public.mw_import_aggregate_numeric_by_date;

CREATE MATERIALIZED VIEW public.mw_import_aggregate_numeric_by_date AS
SELECT uuid_generate_v4() AS id,
       u.name,
       u.locationidentifier,
       u.hierarchyidentifier,
       u.planidentifier,
       u.eventtype,
       u.fieldcode,
       u.datacapturedate,
       u.year,
       u.sum,
       u.avg,
       u.median,
       u.min,
       u.max,
       u.count
FROM (
         -- Existing import aggregation
         SELECT ean2.ancestor                                                                  AS name,
                ean2.ancestor                                                                  AS locationidentifier,
                ean2.hierarchy_identifier                                                      AS hierarchyidentifier,
                ean2.plan_identifier                                                           AS planidentifier,
                ean2.event_type                                                                AS eventtype,
                ean2.field_code                                                                AS fieldcode,
                ean2.data_capture_date                                                         AS datacapturedate,
                COALESCE(date_part('year'::text, ean2.data_capture_date), 0::double precision) AS year,
                sum(ean2.val)                                                                  AS sum,
                avg(ean2.val)                                                                  AS avg,
                percentile_cont(0.5::double precision) WITHIN GROUP (ORDER BY ean2.val)        AS median,
             min(ean2.val)                                                                  AS min,
             max(ean2.val)                                                                  AS max,
             count(ean2.val)                                                                AS count
         FROM import_aggregation_numeric ean2
         GROUP BY ean2.hierarchy_identifier, ean2.ancestor, ean2.plan_identifier, ean2.event_type,
             ean2.field_code, ean2.data_capture_date

         UNION ALL

         -- Raster zonal stats (one row per raster per location)
         SELECT r.location_identifier::text       AS name,
             r.location_identifier::text       AS locationidentifier,
             NULL::varchar                     AS hierarchyidentifier,
             NULL::varchar                     AS planidentifier,
             'RASTER'::varchar                 AS eventtype,
             r.raster_id                       AS fieldcode,
             NULL::timestamp                   AS datacapturedate,
             0::double precision               AS year,
             r.sum                             AS sum,
             r.mean                            AS avg,
             NULL::double precision            AS median,   -- not available in raster stats
             r.min                             AS min,
             r.max                             AS max,
             r.pixel_count                     AS count
         FROM raster_location_zonal_stats r
         WHERE r.entity_status = 'ACTIVE'
     ) u;

CREATE UNIQUE INDEX mw_import_aggregate_numeric_by_date_id_idx
    ON public.mw_import_aggregate_numeric_by_date (id);

CREATE INDEX mw_import_aggregate_numeric_by_date_location_idx
    ON public.mw_import_aggregate_numeric_by_date (locationidentifier);

CREATE INDEX mw_import_aggregate_numeric_by_date_year_idx
    ON public.mw_import_aggregate_numeric_by_date (year);

CREATE INDEX mw_import_aggregate_numeric_by_date_fieldcode_idx
    ON public.mw_import_aggregate_numeric_by_date (fieldcode);

CREATE INDEX mw_import_aggregate_numeric_by_date_hierarchy_idx
    ON public.mw_import_aggregate_numeric_by_date (hierarchyidentifier);

CREATE INDEX mw_import_aggregate_numeric_by_date_composite_idx
    ON public.mw_import_aggregate_numeric_by_date (locationidentifier, year, fieldcode, hierarchyidentifier);