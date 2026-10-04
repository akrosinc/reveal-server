CREATE TABLE IF NOT EXISTS entity_data
    (
        identifier UUID NOT NULL,
        name VARCHAR(255) NOT NULL,
        data   jsonb,
        entity_schema jsonb,
        location_identifier uuid,
    PRIMARY KEY (identifier)
    );
