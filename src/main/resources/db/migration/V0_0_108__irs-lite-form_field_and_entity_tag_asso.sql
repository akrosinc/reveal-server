INSERT INTO form_field(identifier, name, display, form_title, data_type, entity_status, created_by,
                       created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'sprayed', 'sprayed', 'daily_summary', 'integer', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');


INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'irs-lite-sprayed_', 'double', 'irs-lite-sprayed_',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', false, null, null,
        null, 'Date', '', true, false);

INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier
         FROM form_field
         WHERE name = 'sprayed' and form_title = 'daily_summary'),
        (SELECT identifier from entity_tag where tag = 'irs-lite-sprayed_'));

INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'irs-lite-sprayed', 'double', 'irs-lite-sprayed',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"irs-lite-sprayed_",0)',
        ARRAY ['irs-lite-sprayed_'], ARRAY ['sum'], 'date',
        '#get_(#metadata,"irs-lite-sprayed_")', false, true);


INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata, is_aggregate)
VALUES (uuid_generate_v4(), 'irs-lite-sprayed-sum', 'double', 'irs-lite-sprayed-sum',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', false, null, null,
        null, 'plan', '', true, false, true);
