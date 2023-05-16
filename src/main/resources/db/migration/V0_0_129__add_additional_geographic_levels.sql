INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES ('e0f1b4f5-c7d1-4aa9-be9d-b054d11bb15d', 'ward', 'Ward', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING;

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES ('1aaf995e-c837-4d78-9253-1565f75b0a0e', 'county', 'County', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING;

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES ('59300454-1afa-4540-8461-de043028dccd', 'subcounty', 'SubCounty', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING;

