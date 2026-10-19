DROP MATERIALIZED VIEW IF EXISTS public.mw_import_aggregate_numeric_by_date;

CREATE MATERIALIZED VIEW public.mw_import_aggregate_numeric_by_date AS
SELECT md5(concat_ws('|',
                     u.hierarchyidentifier,
                     u.locationidentifier,
                     COALESCE(u.planidentifier, ''),
                     u.eventtype,
                     u.fieldcode,
                     COALESCE(u.datacapturedate::text, '')))::uuid AS id,
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
FROM (SELECT ean2.ancestor                                                                  AS name,
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
      GROUP BY ean2.hierarchy_identifier, ean2.ancestor, ean2.plan_identifier,
          ean2.event_type, ean2.field_code, ean2.data_capture_date
      UNION ALL
      SELECT r.location_identifier::text                                          AS name,
          r.location_identifier::text                                          AS locationidentifier,
          lr.location_hierarchy_identifier::text                               AS hierarchyidentifier,
          NULL::character varying                                              AS planidentifier,
          'RASTER'::character varying                                          AS eventtype,
          r.tag                                                                AS fieldcode,
          r.capture_date                                                       AS datacapturedate,
          COALESCE(date_part('year'::text, r.capture_date), 0::double precision) AS year,
          r.sum,
          r.mean                                                               AS avg,
          NULL::double precision                                               AS median,
          r.min,
          r.max,
          r.pixel_count                                                        AS count
      FROM raster_location_zonal_stats r
          JOIN location_relationship lr ON lr.location_identifier = r.location_identifier
          JOIN location_hierarchy lh ON lh.identifier = lr.location_hierarchy_identifier
          AND lh.hierarchy_status = 'ACTIVE'
      WHERE r.entity_status::text = 'ACTIVE'::text) u;

-- Required for REFRESH MATERIALIZED VIEW CONCURRENTLY (unique, no WHERE clause)
CREATE UNIQUE INDEX mw_import_aggregate_numeric_by_date_id_idx
    ON public.mw_import_aggregate_numeric_by_date (id);

-- findYearRangePerTag / findLatestYearPerTag (hierarchy + tag, aggregating year)
CREATE INDEX mw_import_aggregate_numeric_by_date_hier_field_year_idx
    ON public.mw_import_aggregate_numeric_by_date (hierarchyidentifier, fieldcode, year);

-- findLocationsWithMetadata (location + hierarchy + tag + year)
CREATE INDEX mw_import_aggregate_numeric_by_date_loc_hier_field_year_idx
    ON public.mw_import_aggregate_numeric_by_date (locationidentifier, hierarchyidentifier, fieldcode, year);
