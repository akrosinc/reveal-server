create table if not exists process_tracker
(
    identifier                 uuid                     not null,
    plan_identifier            uuid                     not null,
    process_type               character varying        not null,
    process_trigger_identifier uuid                     not null,
    state                      character varying        not null,
    entity_status              VARCHAR(36)              NOT NULL,
    created_by                 VARCHAR(36)              NOT NULL,
    created_datetime           TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by                VARCHAR(36)              NOT NULL,
    modified_datetime          TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

create table if not exists process_tracker_aud
(
    identifier                 uuid                     not null,
    REV                        INT                      NOT NULL,
    REVTYPE                    INTEGER                  NULL,
    plan_identifier            uuid                     not null,
    process_type               character varying        not null,
    process_trigger_identifier uuid                     not null,
    state                      character varying        not null,
    entity_status              VARCHAR(36)              NOT NULL,
    created_by                 VARCHAR(36)              NOT NULL,
    created_datetime           TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by                VARCHAR(36)              NOT NULL,
    modified_datetime          TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);


create table if not exists task_process_stage
(
    identifier                 uuid                     not null,
    task_identifier            uuid,
    base_entity_identifier     uuid,
    process_tracker_identifier uuid                     not null,
    state                      character varying        not null,
    task_process               character varying        not null,
    entity_status              VARCHAR(36)              NOT NULL,
    created_by                 VARCHAR(36)              NOT NULL,
    created_datetime           TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by                VARCHAR(36)              NOT NULL,
    modified_datetime          TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (process_tracker_identifier) REFERENCES process_tracker (identifier)
);

create table if not exists task_process_stage_aud
(
    identifier                 uuid                     not null,
    REV                        INT                      NOT NULL,
    REVTYPE                    INTEGER                  NULL,
    task_identifier            uuid,
    base_entity_identifier     uuid,
    process_tracker_identifier uuid                     not null,
    state                      character varying        not null,
    task_process               character varying        not null,
    entity_status              VARCHAR(36)              NOT NULL,
    created_by                 VARCHAR(36)              NOT NULL,
    created_datetime           TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by                VARCHAR(36)              NOT NULL,
    modified_datetime          TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);