CREATE TABLE IF NOT EXISTS simulation_request
(
    identifier uuid  NOT NULL,
    request    jsonb NOT NULL,
    PRIMARY KEY (identifier)
);
