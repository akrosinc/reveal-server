CREATE TABLE IF NOT EXISTS country_campaign
(
    identifier        UUID                     NOT NULL,
    name              VARCHAR(255)             NOT NULL,
    groups            JSONB                    NOT NULL,
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS country_campaign_aud
(
    identifier        UUID                     NOT NULL,
    REV               INT                      NOT NULL,
    REVTYPE           INTEGER                  NULL,
    name              VARCHAR(255)             NOT NULL,
    groups            JSONB                    NOT NULL,
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);

insert into country_campaign (identifier, name, groups, entity_status, created_by, created_datetime,
                              modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'Nigeria', '[
  {
    "title": "under five",
    "min": 0,
    "max": 5
  }
]', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');
insert into country_campaign (identifier, name, groups, entity_status, created_by, created_datetime,
                              modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'Rwanda', '[
  {
    "title": "under ten",
    "min": 0,
    "max": 10
  }
]', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');
