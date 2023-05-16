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
VALUES ('e723eeca-b282-4953-ae34-56c914f4f956', 'STH', '[
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
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00')
ON CONFLICT DO NOTHING;

insert into campaign_drug (identifier, name, drugs, entity_status, created_by, created_datetime,
                           modified_by, modified_datetime)
VALUES ('a0e7a5b1-5e58-4b87-aa9b-d6497ea0cebb', 'SCH', '[
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
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00')
ON CONFLICT DO NOTHING;

insert into campaign_drug (identifier, name, drugs, entity_status, created_by, created_datetime,
                           modified_by, modified_datetime)
VALUES ('f7a834ce-5e39-42a4-a3fd-01de492c5fcf', 'Lymphatic filariasis (LF)', '[
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
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00')
ON CONFLICT DO NOTHING;

insert into campaign_drug (identifier, name, drugs, entity_status, created_by, created_datetime,
                           modified_by, modified_datetime)
VALUES ('a0a97945-64fa-4800-b8cc-6d798045848b', 'Onchocerciasis', '[
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
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00')
ON CONFLICT DO NOTHING;

insert into campaign_drug (identifier, name, drugs, entity_status, created_by, created_datetime,
                           modified_by, modified_datetime)
VALUES ('e98965f8-05a2-4178-844b-e899f9cb4128', 'Trachoma', '[
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
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00')
ON CONFLICT DO NOTHING;

insert into campaign_drug (identifier, name, drugs, entity_status, created_by, created_datetime,
                           modified_by, modified_datetime)
VALUES ('6d1bd727-a172-464b-8168-02f1555ea23b', 'Vitamin A', '[
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
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00')
ON CONFLICT DO NOTHING;

insert into campaign_drug (identifier, name, drugs, entity_status, created_by, created_datetime,
                           modified_by, modified_datetime)
VALUES ('f366fd29-23a8-4f68-973d-37ef00040559', 'Seasonal Malaria Chemoprevention', '[
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
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00')
ON CONFLICT DO NOTHING;