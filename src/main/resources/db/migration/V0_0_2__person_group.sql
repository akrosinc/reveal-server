
CREATE TABLE IF NOT EXISTS "group"(
    identifier uuid NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    location_identifier uuid,
    PRIMARY KEY (identifier),
    FOREIGN KEY (location_identifier) REFERENCES location (identifier)
);

CREATE TABLE person
(
    identifier uuid NOT NULL,
    active boolean DEFAULT true,
    name_use character varying NOT NULL,
    name_text character varying NOT NULL,
    name_family character varying NOT NULL,
    name_given character varying NOT NULL,
    name_prefix character varying(30) NOT NULL,
    name_suffix character varying DEFAULT '',
    gender character varying(30) NOT NULL,
    "birthDate" date NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE person_group
(
    person_identifier uuid NOT NULL,
    group_identifier uuid NOT NULL,
    FOREIGN KEY (person_identifier)
        REFERENCES person (identifier),
    FOREIGN KEY (group_identifier)
        REFERENCES "group" (identifier),
    PRIMARY KEY(person_identifier, group_identifier)
);

