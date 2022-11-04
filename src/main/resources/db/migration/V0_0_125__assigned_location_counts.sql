DROP MATERIALIZED VIEW assigned_structure_counts;


CREATE MATERIALIZED VIEW IF NOT EXISTS assigned_structure_counts

AS
SELECT p.identifier AS plan_identifier,
       lp.identifier AS parent_location_identifier,
       lp.name AS parent_location_name,
       pgl.name AS parent_geographic_level_name,
       l.identifier AS location_identifier,
       l.name AS location_name,
       gl.name AS location_geographic_level_name,
       structures.structure_count,
       structures.location_without_survey_location_type,
       structures.water_body_count
FROM plan_locations pl
         LEFT JOIN plan p ON pl.plan_identifier = p.identifier
         LEFT JOIN location_hierarchy lh ON lh.identifier = p.hierarchy_identifier
         LEFT JOIN location l ON l.identifier = pl.location_identifier
         LEFT JOIN geographic_level gl ON gl.identifier = l.geographic_level_identifier
         LEFT JOIN ( SELECT arr.item_object AS parent,
                            arr."position",
                            lr.location_identifier,
                            lr.location_hierarchy_identifier
                     FROM location_relationship lr,
                          LATERAL unnest(lr.ancestry) WITH ORDINALITY arr(item_object, "position")) parents ON parents.location_identifier = l.identifier
         LEFT JOIN location lp ON parents.parent = lp.identifier
         LEFT JOIN geographic_level pgl ON pgl.identifier = lp.geographic_level_identifier
         LEFT JOIN ( SELECT pl_1.identifier AS location_id,
                            pl_1.name,
                            pgl_1.name,
                            count(*) AS structure_count,
                            count(*) FILTER(WHERE l_1.location_property ? 'surveyLocationType'=false) as location_without_survey_location_type,
                             count(*) FILTER(WHERE l_1.location_property ->>'surveyLocationType'='waterbody') as water_body_count
                     FROM ( SELECT arr.item_object AS parent,
                                   arr."position",
                                   lr.location_identifier,
                                   lr.location_hierarchy_identifier
                            FROM location_relationship lr,
                                 LATERAL unnest(lr.ancestry) WITH ORDINALITY arr(item_object, "position")) parents_1
                              LEFT JOIN location l_1 ON l_1.identifier = parents_1.location_identifier
                              LEFT JOIN location pl_1 ON pl_1.identifier = parents_1.parent
                              LEFT JOIN geographic_level gl_1 ON gl_1.identifier = l_1.geographic_level_identifier
                              LEFT JOIN geographic_level pgl_1 ON pgl_1.identifier = pl_1.geographic_level_identifier
                     WHERE gl_1.name::text = 'structure'::text
                     GROUP BY pl_1.identifier, pl_1.name, pgl_1.name) structures(location_id, name, name_1, structure_count,location_without_survey_location_type,water_body_count) ON structures.location_id = l.identifier
WHERE gl.name::text = lh.node_order[array_position(lh.node_order, 'structure'::character varying) - 1]::text;

CREATE UNIQUE INDEX ON assigned_structure_counts ( plan_identifier, parent_location_identifier, location_identifier);