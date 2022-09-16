CREATE
MATERIALIZED VIEW lite_structure_count
AS

SELECT t.location_hierarchy_identifier as location_hierarchy_identifier,
       t.parent_location_identifier as parent_location_identifier,
       t.parent_location_name as parent_location_name,
       sum(t.structure_counts) as structure_counts
from (
         SELECT parents.location_hierarchy_identifier,
                parents.parent                                            AS parent_location_identifier,
                pl.name                                                   AS parent_location_name,
                pgl.name                                                  AS parent_geographic_level_name,
                gl.name                                                   AS geographic_level_name,
                COALESCE((l.location_property ->> 'structures') ::int, 0) as structure_counts
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
     ) as t
group by t.location_hierarchy_identifier, t.parent_location_identifier,
         t.parent_location_name WITH DATA;


CREATE UNIQUE INDEX ON lite_structure_count (location_hierarchy_identifier, parent_location_identifier);