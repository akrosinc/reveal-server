CREATE TABLE IF NOT EXISTS action_template
(
    identifier            UUID                     NOT NULL,
    name                  VARCHAR(255),
    action_template_config jsonb,
    entity_status         VARCHAR(36)              NOT NULL,
    created_by            VARCHAR(36)              NOT NULL,
    created_datetime      TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by           VARCHAR(36)              NOT NULL,
    modified_datetime     TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS action_template_aud
(
    identifier            UUID                     NOT NULL,
    REV                   INT                      NOT NULL,
    REVTYPE               INTEGER                  NULL,
    name                  VARCHAR(255),
    action_template_config jsonb,
    entity_status         VARCHAR(36)              NOT NULL,
    created_by            VARCHAR(36)              NOT NULL,
    created_datetime      TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by           VARCHAR(36)              NOT NULL,
    modified_datetime     TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);
