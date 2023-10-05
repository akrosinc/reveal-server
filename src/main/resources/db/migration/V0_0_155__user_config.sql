CREATE TABLE IF NOT EXISTS user_config
(
    id         uuid                     not null,
    username   character varying        not null,
    createdAt  timestamp with time zone not null,
    received   boolean default false,
    receivedAt timestamp with time zone,
    filename character varying,
    primary key (id)
)
