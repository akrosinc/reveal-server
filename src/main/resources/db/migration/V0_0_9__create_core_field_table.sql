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

INSERT INTO core_field(identifier, field, value_type, lookup_entity_type_identifier, entity_status,
                       created_by, created_datetime, modified_by, modified_datetime)
select 'f7a834ce-5e39-42a4-a3fd-01de492c5fcf',
       'nameFamily',
       'string',
       identifier,
       'ACTIVE',
       '649f338b-eb53-4832-9562-f695e9cc44e7',
       '2022-01-12 13:54:22.106221+00',
       '649f338b-eb53-4832-9562-f695e9cc44e7',
       '2022-01-12 13:54:22.106221+00'
from lookup_entity_type
where code = 'Person'
ON CONFLICT DO NOTHING;

INSERT INTO core_field(identifier, field, value_type, lookup_entity_type_identifier, entity_status,
                       created_by, created_datetime, modified_by, modified_datetime)
select 'a0e7a5b1-5e58-4b87-aa9b-d6497ea0cebb',
       'nameText',
       'string',
       identifier,
       'ACTIVE',
       '649f338b-eb53-4832-9562-f695e9cc44e7',
       '2022-01-12 13:54:22.106221+00',
       '649f338b-eb53-4832-9562-f695e9cc44e7',
       '2022-01-12 13:54:22.106221+00'
from lookup_entity_type
where code = 'Person'
ON CONFLICT DO NOTHING;

INSERT INTO core_field(identifier, field, value_type, lookup_entity_type_identifier, entity_status,
                       created_by, created_datetime, modified_by, modified_datetime)
select 'e723eeca-b282-4953-ae34-56c914f4f956',
       'birthDate',
       'date',
       identifier,
       'ACTIVE',
       '649f338b-eb53-4832-9562-f695e9cc44e7',
       '2022-01-12 13:54:22.106221+00',
       '649f338b-eb53-4832-9562-f695e9cc44e7',
       '2022-01-12 13:54:22.106221+00'
from lookup_entity_type
where code = 'Person'
ON CONFLICT DO NOTHING;