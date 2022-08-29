CREATE TABLE location_above_structure
(
    location_hierarchy_identifier uuid NOT NULL,
    plan_identifier uuid NOT NULL,
    parent_location_identifier uuid NOT NULL,
    parent_location_name character varying NOT NULL,
    parent_geographic_level_name character varying NOT NULL,

    location_above_structure_identifier uuid NOT NULL,
    location_above_structure_name character varying NOT NULL,
    location_above_structure_geographic_level_name character varying NOT NULL,
    is_visited boolean default false,
    is_treated boolean default false,
    is_visited_effectively boolean default false,


    PRIMARY KEY (location_hierarchy_identifier, plan_identifier,parent_location_identifier,location_above_structure_identifier)
);