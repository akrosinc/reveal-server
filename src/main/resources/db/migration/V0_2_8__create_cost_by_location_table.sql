CREATE TABLE IF NOT EXISTS cost_by_location
(
    identifier          UUID  NOT NULL,
    location_identifier UUID,
    action_identifier   UUID,
    cost_container      jsonb,
    calculated_value    DOUBLE PRECISION,
    PRIMARY KEY (identifier),
    FOREIGN KEY (location_identifier) REFERENCES location (identifier),
    FOREIGN KEY (action_identifier) REFERENCES "action" (identifier)
);
