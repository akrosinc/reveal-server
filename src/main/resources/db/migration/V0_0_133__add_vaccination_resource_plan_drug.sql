insert into campaign_drug (identifier, name, drugs, entity_status, created_by, created_datetime,
                           modified_by, modified_datetime)
VALUES ('50528c1e-c9e3-4906-a2b7-4011f975426a', 'Vaccination', '[
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
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00')
ON CONFLICT DO NOTHING;