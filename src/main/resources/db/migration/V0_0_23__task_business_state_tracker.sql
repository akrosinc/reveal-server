CREATE TABLE task_business_state_tracker
(
    identifier uuid NOT NULL,
    parent_location_identifier uuid NOT NULL,
    parent_location_name character varying NOT NULL,
    parent_geographic_level_name character varying NOT NULL,
    plan_identifier uuid NOT NULL,
    location_hierarchy_identifier uuid NOT NULL,
    task_location_identifier uuid NOT NULL,
    task_location_name character varying NOT NULL,
    task_location_geographic_level_name character varying NOT NULL,
    task_business_status character varying NOT NULL,
    task_business_status_update_time TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

