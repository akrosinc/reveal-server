-- ActivityProgress: the evaluated state of a location for a plan, derived from the location's
-- tasks. Computed on the Android client and synced here so it can be reported on.
-- One row per (plan, location).

CREATE SEQUENCE IF NOT EXISTS activity_progress_server_version_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS activity_progress
(
    identifier          UUID        NOT NULL,
    plan_identifier     UUID        NOT NULL,
    location_identifier UUID        NOT NULL,
    state               VARCHAR(32) NOT NULL,
    total               INTEGER     NOT NULL DEFAULT 0,
    not_visited         INTEGER     NOT NULL DEFAULT 0,
    in_progress         INTEGER     NOT NULL DEFAULT 0,
    complete            INTEGER     NOT NULL DEFAULT 0,
    server_version      BIGINT      NOT NULL,
    modified_datetime   TIMESTAMP   NOT NULL DEFAULT now(),
    PRIMARY KEY (identifier),
    FOREIGN KEY (plan_identifier) REFERENCES plan (identifier),
    FOREIGN KEY (location_identifier) REFERENCES location (identifier),
    CONSTRAINT uq_activity_progress_plan_location UNIQUE (plan_identifier, location_identifier)
);

CREATE INDEX IF NOT EXISTS idx_activity_progress_plan_server_version
    ON activity_progress (plan_identifier, server_version);

CREATE INDEX IF NOT EXISTS idx_activity_progress_location
    ON activity_progress (location_identifier);
