DROP MATERIALIZED VIEW location_counts;

CREATE MATERIALIZED VIEW IF NOT EXISTS location_counts
    TABLESPACE pg_default
AS
SELECT parents.location_hierarchy_identifier,
       parents.parent                                                                     AS parent_location_identifier,
       pl.name                                                                            AS parent_location_name,
       pgl.name                                                                           AS parent_geographic_level_name,
       gl.name                                                                            AS geographic_level_name,
       count(*)                                                                           AS location_count,
       count(*) FILTER (WHERE l.location_property ->> 'surveyLocationType' is null OR
                              l.location_property ? 'surveyLocationType' =
                              false)                                                      as location_without_survey_location_type,
       count(*)
       FILTER (WHERE l.location_property ->> 'surveyLocationType' = 'waterbody')          as water_body_count
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
GROUP BY parents.location_hierarchy_identifier, parents.parent, pl.name, pgl.name, gl.name;

CREATE UNIQUE INDEX ON location_counts (location_hierarchy_identifier, parent_location_identifier,
                                        geographic_level_name);