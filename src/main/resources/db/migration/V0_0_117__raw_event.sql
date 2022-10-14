CREATE TABLE raw_event
(
    identifier uuid NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    event_string character varying not null,
    PRIMARY KEY (identifier)
);
