INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-male-1-to-4-ALB', 'integer', 'treated-male-1-to-4',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-male-1-to-4",0) and #eq_(#metadata,"mda-lite-drugs","ALB")',
        ARRAY['mda-lite-drugs', 'treated-male-1-to-4'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-male-1-to-4")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-male-5-to-14-ALB', 'integer', 'treated-male-5-to-14',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-male-5-to-14",0) and #eq_(#metadata,"mda-lite-drugs","ALB")',
        ARRAY['mda-lite-drugs', 'treated-male-5-to-14'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-male-5-to-14")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-male-above-15-ALB', 'integer', 'treated_male_above_15',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-male-above-15",0) and #eq_(#metadata,"mda-lite-drugs","ALB")',
        ARRAY['mda-lite-drugs', 'treated-male-above-15'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-male-above-15")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'total-males-ALB', 'integer', 'total_males',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"total-males",0) and #eq_(#metadata,"mda-lite-drugs","ALB")',
        ARRAY['mda-lite-drugs', 'total-males'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"total-males")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-female-1-to-4-ALB', 'integer', 'treated_female_1_to_4',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-female-1-to-4",0) and #eq_(#metadata,"mda-lite-drugs","ALB")',
        ARRAY['mda-lite-drugs', 'treated-female-1-to-4'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-female-1-to-4")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-female-5-to-14-ALB', 'integer', 'treated_female_5_to_14',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-female-5-to-14",0) and #eq_(#metadata,"mda-lite-drugs","ALB")',
        ARRAY['mda-lite-drugs', 'treated-female-5-to-14'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-female-5-to-14")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-female-above-15-ALB', 'integer', 'treated_female_above_15',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-female-above-15",0) and #eq_(#metadata,"mda-lite-drugs","ALB")',
        ARRAY['mda-lite-drugs', 'treated-female-above-15'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-female-above-15")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'total-female-ALB', 'integer', 'total_female',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"total-female",0) and #eq_(#metadata,"mda-lite-drugs","ALB")',
        ARRAY['mda-lite-drugs', 'total-female'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"total-female")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'total-people-ALB', 'integer', 'total-people',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '(#gt_(#metadata,"total-female",0) and #gt_(#metadata,"total-males",0)) and #eq_(#metadata,"mda-lite-drugs","ALB")',
        ARRAY['mda-lite-drugs', 'total-female-ALB', 'total-male-ALB'], ARRAY['sum'], 'Date',
        '((#get_(#metadata,"total-female")==null?0:#get_(#metadata,"total-female")) + (#get_(#metadata,"total-males")==null?0:#get_(#metadata,"total-males")))',
        false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'mda-lite-administered-ALB', 'integer', 'total_tablet_admin_max',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"mda-lite-administered",0) and #eq_(#metadata,"mda-lite-drugs","ALB")',
        ARRAY['mda-lite-drugs', 'mda-lite-administered'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"mda-lite-administered")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'mda-lite-adverse-ALB', 'integer', 'adverse',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"mda-lite-adverse",0) and #eq_(#metadata,"mda-lite-drugs","ALB")',
        ARRAY['mda-lite-drugs', 'mda-lite-adverse'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"mda-lite-adverse")', false, false);

INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-male-1-to-4-MEB', 'integer', 'treated-male-1-to-4',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-male-1-to-4",0) and #eq_(#metadata,"mda-lite-drugs","MEB")',
        ARRAY['mda-lite-drugs', 'treated-male-1-to-4'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-male-1-to-4")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-male-5-to-14-MEB', 'integer', 'treated-male-5-to-14',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-male-5-to-14",0) and #eq_(#metadata,"mda-lite-drugs","MEB")',
        ARRAY['mda-lite-drugs', 'treated-male-5-to-14'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-male-5-to-14")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-male-above-15-MEB', 'integer', 'treated_male_above_15',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-male-above-15",0) and #eq_(#metadata,"mda-lite-drugs","MEB")',
        ARRAY['mda-lite-drugs', 'treated-male-above-15'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-male-above-15")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'total-males-MEB', 'integer', 'total_males',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"total-males",0) and #eq_(#metadata,"mda-lite-drugs","MEB")',
        ARRAY['mda-lite-drugs', 'total-males'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"total-males")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-female-1-to-4-MEB', 'integer', 'treated_female_1_to_4',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-female-1-to-4",0) and #eq_(#metadata,"mda-lite-drugs","MEB")',
        ARRAY['mda-lite-drugs', 'treated-female-1-to-4'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-female-1-to-4")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-female-5-to-14-MEB', 'integer', 'treated_female_5_to_14',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-female-5-to-14",0) and #eq_(#metadata,"mda-lite-drugs","MEB")',
        ARRAY['mda-lite-drugs', 'treated-female-5-to-14'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-female-5-to-14")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-female-above-15-MEB', 'integer', 'treated_female_above_15',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-female-above-15",0) and #eq_(#metadata,"mda-lite-drugs","MEB")',
        ARRAY['mda-lite-drugs', 'treated-female-above-15'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-female-above-15")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'total-female-MEB', 'integer', 'total_female',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"total-female",0) and #eq_(#metadata,"mda-lite-drugs","MEB")',
        ARRAY['mda-lite-drugs', 'total-female'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"total-female")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'total-people-MEB', 'integer', 'total-people',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '(#gt_(#metadata,"total-female",0) and #gt_(#metadata,"total-males",0)) and #eq_(#metadata,"mda-lite-drugs","MEB")',
        ARRAY['mda-lite-drugs', 'total-female-MEB', 'total-male-MEB'], ARRAY['sum'], 'Date',
        '((#get_(#metadata,"total-female")==null?0:#get_(#metadata,"total-female")) + (#get_(#metadata,"total-males")==null?0:#get_(#metadata,"total-males")))',
        false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'mda-lite-administered-MEB', 'integer', 'total_tablet_admin_max',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"mda-lite-administered",0) and #eq_(#metadata,"mda-lite-drugs","MEB")',
        ARRAY['mda-lite-drugs', 'mda-lite-administered'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"mda-lite-administered")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'mda-lite-adverse-MEB', 'integer', 'adverse',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"mda-lite-adverse",0) and #eq_(#metadata,"mda-lite-drugs","MEB")',
        ARRAY['mda-lite-drugs', 'mda-lite-adverse'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"mda-lite-adverse")', false, false);

INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-male-1-to-4-PZQ', 'integer', 'treated-male-1-to-4',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-male-1-to-4",0) and #eq_(#metadata,"mda-lite-drugs","PZQ")',
        ARRAY['mda-lite-drugs', 'treated-male-1-to-4'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-male-1-to-4")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-male-5-to-14-PZQ', 'integer', 'treated-male-5-to-14',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-male-5-to-14",0) and #eq_(#metadata,"mda-lite-drugs","PZQ")',
        ARRAY['mda-lite-drugs', 'treated-male-5-to-14'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-male-5-to-14")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-male-above-15-PZQ', 'integer', 'treated_male_above_15',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-male-above-15",0) and #eq_(#metadata,"mda-lite-drugs","PZQ")',
        ARRAY['mda-lite-drugs', 'treated-male-above-15'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-male-above-15")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'total-males-PZQ', 'integer', 'total_males',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"total-males",0) and #eq_(#metadata,"mda-lite-drugs","PZQ")',
        ARRAY['mda-lite-drugs', 'total-males'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"total-males")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-female-1-to-4-PZQ', 'integer', 'treated_female_1_to_4',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-female-1-to-4",0) and #eq_(#metadata,"mda-lite-drugs","PZQ")',
        ARRAY['mda-lite-drugs', 'treated-female-1-to-4'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-female-1-to-4")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-female-5-to-14-PZQ', 'integer', 'treated_female_5_to_14',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-female-5-to-14",0) and #eq_(#metadata,"mda-lite-drugs","PZQ")',
        ARRAY['mda-lite-drugs', 'treated-female-5-to-14'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-female-5-to-14")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'treated-female-above-15-PZQ', 'integer', 'treated_female_above_15',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"treated-female-above-15",0) and #eq_(#metadata,"mda-lite-drugs","PZQ")',
        ARRAY['mda-lite-drugs', 'treated-female-above-15'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"treated-female-above-15")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'total-female-PZQ', 'integer', 'total_female',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"total-female",0) and #eq_(#metadata,"mda-lite-drugs","PZQ")',
        ARRAY['mda-lite-drugs', 'total-female'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"total-female")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'total-people-PZQ', 'integer', 'total-people',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '(#gt_(#metadata,"total-female",0) and #gt_(#metadata,"total-males",0)) and #eq_(#metadata,"mda-lite-drugs","PZQ")',
        ARRAY['mda-lite-drugs', 'total-female-PZQ', 'total-male-PZQ'], ARRAY['sum'], 'Date',
        '((#get_(#metadata,"total-female")==null?0:#get_(#metadata,"total-female")) + (#get_(#metadata,"total-males")==null?0:#get_(#metadata,"total-males")))',
        false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'mda-lite-administered-PZQ', 'integer', 'total_tablet_admin_max',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"total-tablet-admin-max",0) and #eq_(#metadata,"mda-lite-drugs","PZQ")',
        ARRAY['mda-lite-drugs', 'mda-lite-administered'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"mda-lite-administered")', false, false);
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'mda-lite-adverse-PZQ', 'integer', 'adverse',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true,
        '#gt_(#metadata,"mda-lite-adverse",0) and #eq_(#metadata,"mda-lite-drugs","PZQ")',
        ARRAY['mda-lite-drugs', 'mda-lite-adverse'], ARRAY['sum'], 'Date',
        '#get_(#metadata,"mda-lite-adverse")', false, false);

INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'drugs'),
        (SELECT identifier from entity_tag where tag = 'mda-lite-drugs'));
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'treated_male_1_to_4'),
        (SELECT identifier from entity_tag where tag = 'treated-male-1-to-4'));
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'treated_male_5_to_14'),
        (SELECT identifier from entity_tag where tag = 'treated-male-5-to-14'));
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'treated_male_above_15'),
        (SELECT identifier from entity_tag where tag = 'treated-male-above-15'));
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'total_males'),
        (SELECT identifier from entity_tag where tag = 'total-males'));
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'treated_female_1_to_4'),
        (SELECT identifier from entity_tag where tag = 'treated-female-1-to-4'));
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'treated_female_5_to_14'),
        (SELECT identifier from entity_tag where tag = 'treated-female-5-to-14'));
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'treated_female_above_15'),
        (SELECT identifier from entity_tag where tag = 'treated-female-above-15'));
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'total_female'),
        (SELECT identifier from entity_tag where tag = 'total-female'));
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'administered'),
        (SELECT identifier from entity_tag where tag = 'mda-lite-administered'));
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'adverse'),
        (SELECT identifier from entity_tag where tag = 'mda-lite-adverse'));

