INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES ('c81e5ae0-95c4-409c-9deb-c0a6d0649f23', 'posto', 'Posto', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING;

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES ('179a3075-f799-4f31-82ed-8702868afbdc', 'localities', 'Localities', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING;