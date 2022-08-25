CREATE
MATERIALIZED VIEW IF NOT EXISTS location_relationships
TABLESPACE pg_default
AS
SELECT  parents.location_hierarchy_identifier,
       parents.parent AS location_parent_identifier,
       pgl.name       AS parent_geographic_level_name,
       parents.location_identifier,
       gl.name        AS geographic_level_name
FROM (SELECT arr.item_object AS parent,
             arr."position",
             lr.location_identifier,
             lr.location_hierarchy_identifier
      FROM location_relationship lr,
           LATERAL unnest(lr.ancestry) WITH ORDINALITY arr(item_object, "position")) parents
         LEFT JOIN location l ON parents.location_identifier = l.identifier
         LEFT JOIN location pl ON parents.parent = pl.identifier
         LEFT JOIN geographic_level pgl ON pgl.identifier = pl.geographic_level_identifier
         LEFT JOIN geographic_level gl
                   ON gl.identifier = l.geographic_level_identifier WITH NO DATA;


CREATE
MATERIALIZED VIEW IF NOT EXISTS location_counts
TABLESPACE pg_default
AS
SELECT parents.location_hierarchy_identifier,
       parents.parent AS parent_location_identifier,
       pl.name        as parent_location_name,
       pgl.name       AS parent_geographic_level_name,
       gl.name        AS geographic_level_name,
       count(*)       as location_count
FROM (SELECT arr.item_object AS parent,
             arr."position",
             lr.location_identifier,
             lr.location_hierarchy_identifier
      FROM location_relationship lr,
           LATERAL unnest(lr.ancestry) WITH ORDINALITY arr(item_object, "position")) parents
         LEFT JOIN location l ON parents.location_identifier = l.identifier
         LEFT JOIN location pl ON parents.parent = pl.identifier
         LEFT JOIN geographic_level pgl ON pgl.identifier = pl.geographic_level_identifier
         LEFT JOIN geographic_level gl ON gl.identifier = l.geographic_level_identifier
group by parents.location_hierarchy_identifier, parents.parent, pl.name, pgl.name,
         gl.name WITH DATA;

CREATE UNIQUE INDEX ON location_counts (location_hierarchy_identifier, parent_location_identifier, geographic_level_name);