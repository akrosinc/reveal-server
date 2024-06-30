alter table IF EXISTS entity_tag
    add column if not exists upload_geographic_level_identifier uuid;

DROP TABLE IF EXISTS entity_tag_aud;
-- auto-generated definition
create table IF NOT EXISTS entity_tag_aud
(
    identifier                         uuid                  not null,
    rev                                integer               not null,
    revtype                            integer,
    tag                                varchar               not null,
    value_type                         varchar               not null,
    definition                         varchar,
    aggregation_method                 character varying[],
    is_aggregate                       boolean default false not null,
    simulation_display                 boolean default false,
    metadata_import_id                 uuid,
    referenced_tag                     uuid,
    is_public                          boolean default false,
    upload_geographic_level_identifier uuid,
    is_deleting                        boolean,
    primary key (identifier, rev)
);
-- auto-generated definition
create table IF NOT EXISTS  entity_tag_ownership_aud
(
    id            uuid not null,
    rev                                integer               not null,
    revtype                            integer,
    entity_tag_id uuid not null,
    user_sid      uuid not null,
    primary key (id, rev)
);

create table IF NOT EXISTS  entity_tag_acc_grants_user_aud
(
    id            uuid not null,
    rev                                integer               not null,
    revtype                            integer,
    entity_tag_id uuid not null,
    user_sid      uuid not null,
    primary key (id, rev)
);


create table IF NOT EXISTS  entity_tag_acc_grants_organization_aud
(
    id              uuid not null,
    rev                                integer               not null,
    revtype                            integer,
    entity_tag_id   uuid not null,
    organization_id uuid not null,
    primary key (id, rev)
);
