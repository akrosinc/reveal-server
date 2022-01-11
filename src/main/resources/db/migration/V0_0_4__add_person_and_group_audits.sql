CREATE TABLE IF NOT EXISTS "group_aud"
(
    identifier          uuid                   NOT NULL,
    name                character varying(255) NOT NULL,
    type                character varying(255) NOT NULL,
    rev                 integer                NOT NULL,
    revtype             integer,
    location_identifier uuid,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier,rev),
    FOREIGN KEY (location_identifier) REFERENCES location (identifier)
);

CREATE TABLE person_aud
(
    identifier uuid NOT NULL,
    rev                 integer                NOT NULL,
    revtype             integer,
    active boolean DEFAULT true,
    name_use character varying NOT NULL,
    name_text character varying NOT NULL,
    name_family character varying NOT NULL,
    name_given character varying NOT NULL,
    name_prefix character varying(30) NOT NULL,
    name_suffix character varying DEFAULT '',
    gender character varying(30) NOT NULL,
    "birthDate" date NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier,rev)
);

CREATE TABLE person_group_aud
(
    rev                 integer                NOT NULL,
    revtype             integer,
    person_identifier uuid NOT NULL,
    group_identifier uuid NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (person_identifier)
        REFERENCES person (identifier),
    FOREIGN KEY (group_identifier)
        REFERENCES "group" (identifier),
    PRIMARY KEY(person_identifier, group_identifier,rev)
);