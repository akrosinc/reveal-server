create schema IF NOT EXISTS amdr;

create table IF NOT EXISTS amdr.amdr_data
(
    id               uuid                    not null
    constraint amdr_data_pk
    primary key,
    location_id      uuid                    not null,
    data             jsonb                   not null,
    type             varchar                 not null,
    overall_value    varchar                 not null,
    datetime         timestamp default now() not null,
    overall_object   jsonb,
    collection_year  text,
    collection_month text,
    constraint amdr_data_unique_location_year_type
    unique (location_id, collection_year, type)
);

create table IF NOT EXISTS amdr.amdr_mappings
(
    id            serial
    constraint amdr_mappings_pk
    primary key,
    amdr_key      varchar not null,
    amdr_sub_keys jsonb   not null
);

create table IF NOT EXISTS amdr.amdr_header_names
(
    key  text not null
    constraint amdr_header_names_pk
    primary key,
    name text not null
);

create table IF NOT EXISTS amdr.amdr_import
(
    identifier        uuid                     not null
    primary key,
    filename          varchar(255)             not null,
    uploaded_datetime timestamp with time zone not null,
                                    uploaded_by       varchar(255)             not null,
    status            varchar(255),
    entity_status     varchar(36)              not null,
    created_by        varchar(36)              not null,
    created_datetime  timestamp with time zone not null,
                                    modified_by       varchar(36)              not null,
    modified_datetime timestamp with time zone not null
                                    );

CREATE table IF NOT EXISTS amdr.amdr_import_aud
(
    identifier        uuid                     not null,
    rev               integer                  not null,
    revtype           integer,
    filename          varchar(255)             not null,
    uploaded_datetime timestamp with time zone not null,
                                    uploaded_by       varchar(255)             not null,
    status            varchar(255),
    entity_status     varchar(36)              not null,
    created_by        varchar(36)              not null,
    created_datetime  timestamp with time zone not null,
                                    modified_by       varchar(36)              not null,
    modified_datetime timestamp with time zone not null,
                                    primary key (identifier, rev)
    );

CREATE TABLE IF NOT EXISTS amdr.amdr_sample_data
(
    sample_internal_id  text not null
    primary key,
    kelch               text,
    pfcrt_72            text,
    pfcrt_74            text,
    pfcrt_75            text,
    pfcrt_76            text,
    pfdhfr_51           text,
    pfdhfr_59           text,
    pfdhfr_108          text,
    pfdhfr_164          text,
    pfdhps_436          text,
    pfdhps_437          text,
    pfdhps_540          text,
    pfdhps_581          text,
    pfdhps_613          text,
    pfmdr1_86           text,
    pfmdr1_184          text,
    pfmdr1_1246         text,
    region              text,
    date_collection     timestamp default now(),
    status              varchar(255),
    location_identifier text,
    import_id           uuid
    );
