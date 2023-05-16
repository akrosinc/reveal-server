CREATE TABLE IF NOT EXISTS performance_user_type
(
    identifier      uuid              NOT NULL,
    plan_identifier uuid              not null,
    user_string     character varying not null,
    type_string     character varying not null,
    PRIMARY KEY (identifier),
    unique (plan_identifier, user_string)
);