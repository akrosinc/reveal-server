CREATE TABLE IF NOT EXISTS simulation (
    identifier UUID PRIMARY KEY,
    plan_identifier UUID UNIQUE NOT NULL,
    FOREIGN KEY (plan_identifier) REFERENCES plan(identifier) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS dataset (
    identifier UUID PRIMARY KEY,
    simulation_identifier UUID NOT NULL,
    entity_tag_identifier UUID NOT NULL,
    hex_color VARCHAR(7) NOT NULL,
    line_width INTEGER NOT NULL,
    name CHARACTER VARYING NOT NULL,
    FOREIGN KEY (simulation_identifier) REFERENCES simulation(identifier) ON DELETE CASCADE,
    FOREIGN KEY (entity_tag_identifier) REFERENCES entity_tag(identifier) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS simulation_aud (
    rev INT NOT NULL,
    revtype INTEGER,
    identifier UUID,
    plan_identifier UUID,
    PRIMARY KEY (identifier, rev),
    FOREIGN KEY (plan_identifier) REFERENCES plan(identifier)
);
