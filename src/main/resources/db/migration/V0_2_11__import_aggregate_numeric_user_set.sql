-- import_aggregate_numeric: keep the full statistics for 'import' rows (dataset uploads) and add
-- 'user-set' rows (counts set per location) with only the sum; avg, median, min and max are null.
DROP MATERIALIZED VIEW IF EXISTS import_aggregate_numeric;

CREATE MATERIALIZED VIEW import_aggregate_numeric
AS
SELECT nextval('import_aggregate_numeric_seq'::regclass) AS id,
       agg.*
FROM (SELECT ean2.ancestor                                        AS name,
             ean2.ancestor::character varying                     AS locationidentifier,
             ean2.hierarchy_identifier                            AS hierarchyidentifier,
             ean2.plan_identifier::character varying              AS planidentifier,
             ean2.event_type                                      AS eventtype,
             ean2.field_code                                      AS fieldcode,
             sum(ean2.val::double precision)                      AS sum,
             avg(ean2.val::double precision)                      AS avg,
             percentile_cont(0.5::double precision)
             WITHIN GROUP (ORDER BY (ean2.val::double precision)) AS median,
             min(ean2.val::double precision)                      AS min,
             max(ean2.val::double precision)                      AS max
      FROM import_aggregation_numeric ean2
      WHERE ean2.event_type = 'import'
      GROUP BY ean2.hierarchy_identifier, ean2.ancestor, ean2.plan_identifier,
               ean2.event_type, ean2.field_code

      UNION ALL

      SELECT ean2.ancestor                           AS name,
             ean2.ancestor::character varying        AS locationidentifier,
             ean2.hierarchy_identifier               AS hierarchyidentifier,
             ean2.plan_identifier::character varying AS planidentifier,
             ean2.event_type                         AS eventtype,
             ean2.field_code                         AS fieldcode,
             sum(ean2.val::double precision)         AS sum,
             NULL::double precision                  AS avg,
             NULL::double precision                  AS median,
             NULL::double precision                  AS min,
             NULL::double precision                  AS max
      FROM import_aggregation_numeric ean2
      WHERE ean2.event_type = 'user-set'
      GROUP BY ean2.hierarchy_identifier, ean2.ancestor, ean2.plan_identifier,
               ean2.event_type, ean2.field_code) agg
WITH DATA;

CREATE UNIQUE INDEX IF NOT EXISTS import_aggregate_numeric_id_idx
    ON import_aggregate_numeric (id);

CREATE INDEX IF NOT EXISTS import_aggregate_numeric_location_idx
    ON import_aggregate_numeric USING btree (locationidentifier);
