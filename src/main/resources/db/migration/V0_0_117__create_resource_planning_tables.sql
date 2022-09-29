CREATE TABLE IF NOT EXISTS country_campaign
(
    identifier        UUID                     NOT NULL,
    name              VARCHAR(255)             NOT NULL,
    key               VARCHAR(255)             NOT NULL,
    groups            JSONB                    NOT NULL,
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);


CREATE TABLE IF NOT EXISTS campaign_drug
(
    identifier        UUID                     NOT NULL,
    name              VARCHAR(255)             NOT NULL,
    drugs             JSONB                    NOT NULL,
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS resource_planning_history
(
    identifier        UUID                     NOT NULL,
    name              VARCHAR(255)             NOT NULL,
    history           JSONB                    NOT NULL,
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS resource_planning_history_aud
(
    identifier        UUID                     NOT NULL,
    REV               INT                      NOT NULL,
    REVTYPE           INTEGER                  NULL,
    name              VARCHAR(255)             NOT NULL,
    history           JSONB                    NOT NULL,
    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);


insert into campaign_drug (identifier, name, drugs, entity_status, created_by, created_datetime,
                           modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'STH', '[
  {
    "name": "Albendazole (ALB)",
    "min": 0.5,
    "max": 5.5,
    "half": true,
    "full": false,
    "millis": false,
    "key": "alb"
  },
  {
    "name": "Mebendazole (MEB)",
    "min": 0.5,
    "max": 5.5,
    "half": true,
    "full": false,
    "millis": false,
    "key": "meb"
  }
]', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');

insert into campaign_drug (identifier, name, drugs, entity_status, created_by, created_datetime,
                           modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'SCH', '[
  {
    "name": "Praziquantel (PZQ)",
    "min": 0.5,
    "max": 5.5,
    "half": true,
    "full": false,
    "millis": false,
    "key": "pzq"
  }
]', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');

insert into campaign_drug (identifier, name, drugs, entity_status, created_by, created_datetime,
                           modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'Lymphatic filariasis (LF)', '[
  {
    "name": "Albendazole (ALB)",
    "min": 0.5,
    "max": 5.5,
    "half": true,
    "full": false,
    "millis": false,
    "key": "alb"
  },
  {
    "name": "Ivermectin",
    "min": 0.5,
    "max": 5.5,
    "half": true,
    "full": false,
    "millis": false,
    "key": "ivr"
  }
]', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');

insert into campaign_drug (identifier, name, drugs, entity_status, created_by, created_datetime,
                           modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'Onchocerciasis', '[
  {
    "name": "Moxidectin",
    "min": 0.5,
    "max": 5.5,
    "half": true,
    "full": false,
    "millis": false,
    "key": "mox"
  },
  {
    "name": "Ivermectin",
    "min": 0.5,
    "max": 5.5,
    "half": true,
    "full": false,
    "millis": false,
    "key": "ivr"
  }
]', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');

insert into campaign_drug (identifier, name, drugs, entity_status, created_by, created_datetime,
                           modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'Trachoma', '[
  {
    "name": "Azithromycin",
    "min": 0.5,
    "max": 5.5,
    "half": true,
    "full": false,
    "millis": false,
    "key": "azt"
  }
]', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');

insert into campaign_drug (identifier, name, drugs, entity_status, created_by, created_datetime,
                           modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'Vitamin A', '[
  {
    "name": "Vitamin A",
    "min": 1,
    "max": 4,
    "half": false,
    "full": true,
    "millis": false,
    "key": "vtm-a"
  }
]', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');

insert into campaign_drug (identifier, name, drugs, entity_status, created_by, created_datetime,
                           modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'Seasonal Malaria Chemoprevention', '[
  {
    "name": "Amodiaquine (AQ)",
    "min": 0.5,
    "max": 3.0,
    "half": true,
    "full": false,
    "millis": false,
    "key": "aq"
  },
  {
    "name": "Sulfadoxine–pyrimethamine (SP)",
    "min": 0.5,
    "max": 1.0,
    "half": true,
    "full": false,
    "millis": false,
    "key": "sp"
  }
]', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');