CREATE TABLE IF NOT EXISTS performance_event_tracker
(
    identifier      uuid              NOT NULL,
    submission_id   character varying NOT NULL,
    user_data       jsonb             not null,
    plan_identifier uuid              not null,
    PRIMARY KEY (identifier),
    unique (submission_id)
);