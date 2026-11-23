INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'site', 'Site', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', now(),
        '71fca736-c156-40bc-9de5-3ae04981fbc9', now()) ON CONFLICT DO NOTHING;

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'sitepoint', 'Site Point', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', now(),
        '71fca736-c156-40bc-9de5-3ae04981fbc9', now()) ON CONFLICT DO NOTHING;
