CREATE TABLE IF NOT EXISTS plan_target_type
(
    identifier                  uuid                     NOT NULL,
    plan_identifier             uuid                     NOT NULL,
    geographic_level_identifier uuid                     NOT NULL,
    entity_status               VARCHAR(36)              NOT NULL,
    created_by                  VARCHAR(36)              NOT NULL,
    created_datetime            TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by                 VARCHAR(36)              NOT NULL,
    modified_datetime           TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier),
    UNIQUE (plan_identifier),
    FOREIGN KEY (plan_identifier)
        REFERENCES plan (identifier),
    FOREIGN KEY (geographic_level_identifier)
        REFERENCES geographic_level (identifier)
);
CREATE TABLE IF NOT EXISTS  plan_target_type_aud
(
    identifier                  uuid                     NOT NULL,
    REV                        INT                      NOT NULL,
    REVTYPE                    INTEGER                  NULL,
    plan_identifier             uuid                     NOT NULL,
    geographic_level_identifier uuid                     NOT NULL,
    entity_status               VARCHAR(36)              NOT NULL,
    created_by                  VARCHAR(36)              NOT NULL,
    created_datetime            TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by                 VARCHAR(36)              NOT NULL,
    modified_datetime           TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);
INSERT INTO lookup_intervention_type(identifier, name, code, entity_status, created_by,
                                     created_datetime, modified_by, modified_datetime)
VALUES ('b5c71358-bda3-414c-88e9-0ceacdb736d0', 'MDA Lite', 'MDA Lite',
        'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00')  ON CONFLICT DO NOTHING ;

INSERT INTO lookup_intervention_type(identifier, name, code, entity_status, created_by,
                                     created_datetime, modified_by, modified_datetime)
VALUES ('e6d7f598-ce98-43ec-9612-0fd82ccf651c', 'IRS Lite', 'IRS Lite',
        'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00')  ON CONFLICT DO NOTHING ;