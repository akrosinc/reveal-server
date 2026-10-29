CREATE TABLE IF NOT EXISTS action_location_recapture_limit
(
    identifier          UUID  NOT NULL,
    instance_identifier UUID,
    location_identifier UUID,
    action_identifier   UUID,
    count               INTEGER,
    PRIMARY KEY (identifier),
    FOREIGN KEY (instance_identifier) REFERENCES instance (identifier),
    FOREIGN KEY (location_identifier) REFERENCES location (identifier),
    FOREIGN KEY (action_identifier) REFERENCES "action" (identifier)
);
