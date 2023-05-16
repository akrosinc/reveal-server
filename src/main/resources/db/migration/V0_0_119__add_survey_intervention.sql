INSERT INTO lookup_intervention_type(identifier, name, code, entity_status, created_by,
                                     created_datetime, modified_by, modified_datetime)
VALUES ('5f800365-73da-4a52-9788-c920f5401693', 'SURVEY', 'SURVEY',
        'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9', current_timestamp,
        '71fca736-c156-40bc-9de5-3ae04981fbc9', current_timestamp)
ON CONFLICT DO NOTHING;