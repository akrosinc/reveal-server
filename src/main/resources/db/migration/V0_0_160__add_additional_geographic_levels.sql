INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'region', 'Region', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING;

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'zone', 'Zone', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING;

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'cluster', 'Cluster', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING;

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'hamlet', 'Hamlet', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING;

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'village', 'Village', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING;
