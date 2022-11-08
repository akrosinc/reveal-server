CREATE TABLE IF NOT EXISTS event_tracker
(
    identifier          uuid              NOT NULL,
    aggregation_key     character varying NOT NULL,
    location_identifier uuid,
    plan_identifier     uuid              NOT NULL,
    event_type          uuid              NOT NULL,
    task_identifier     uuid,
    observations        jsonb             not null,
    supervisor          character varying,
    device_user         character varying,
    operation_datetime  character varying,
    contributing_events uuid[],
    PRIMARY KEY (identifier),
    unique(aggregation_key)
);

