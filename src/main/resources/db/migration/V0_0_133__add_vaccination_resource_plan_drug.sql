insert into campaign_drug (identifier, name, drugs, entity_status, created_by, created_datetime,
                           modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'Vaccination', '[
  {
    "name": "Vaccine",
    "min": 0.1,
    "max": 2.0,
    "half": true,
    "full": false,
    "millis": true,
    "key": "vc"
  }
]', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');