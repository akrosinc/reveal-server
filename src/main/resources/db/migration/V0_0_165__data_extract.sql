CREATE TABLE IF NOT EXISTS data_extract_query
(
    id              uuid              not null,
    plan_identifier uuid              not null,
    query           character varying not null,
    primary key (id)
);
