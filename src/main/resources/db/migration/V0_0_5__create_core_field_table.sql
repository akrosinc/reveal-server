CREATE TABLE IF NOT EXISTS core_field
(
    identifier                    uuid                     NOT NULL,
    field                         character varying        NOT NULL,
    value_type                    character varying        NOT NULL,
    definition                    character varying,
    lookup_entity_type_identifier uuid                     NOT NULL,
    entity_status                 character varying(36)    NOT NULL,
    created_by                    character varying(36)    NOT NULL,
    created_datetime              timestamp with time zone NOT NULL,
    modified_by                   character varying(36)    NOT NULL,
    modified_datetime             timestamp with time zone NOT NULL,
    PRIMARY KEY (identifier),
    FOREIGN KEY (lookup_entity_type_identifier) REFERENCES lookup_entity_type (identifier)
);

CREATE TABLE IF NOT EXISTS core_field_aud
(
    identifier                    uuid                     NOT NULL,
    rev                           integer                  NOT NULL,
    revtype                       integer,
    field                         character varying        NOT NULL,
    value_type                    character varying        NOT NULL,
    definition                    character varying,
    lookup_entity_type_identifier uuid                     NOT NULL,
    entity_status                 character varying(36)    NOT NULL,
    created_by                    character varying(36)    NOT NULL,
    created_datetime              timestamp with time zone NOT NULL,
    modified_by                   character varying(36)    NOT NULL,
    modified_datetime             timestamp with time zone NOT NULL,
    PRIMARY KEY (identifier, rev)
);

-- INSERT INTO core_field (identifier, field, value_type, definition, lookup_entity_type_identifier, entity_status, created_by, created_datetime, modified_by, modified_datetime) VALUES
-- (uuid_generate_v4(), 'birth_date', 'date', '', '96f6b48e-46a7-492b-a519-9d87fff6ee1a', 'ACTIVE', '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');
-- INSERT INTO core_field (identifier, field, value_type, definition, lookup_entity_type_identifier, entity_status, created_by, created_datetime, modified_by, modified_datetime) VALUES
--     (uuid_generate_v4(), 'gender', 'string', '', '96f6b48e-46a7-492b-a519-9d87fff6ee1a', 'ACTIVE', '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');