--cdd drug allocation

INSERT INTO form_field(identifier, name, display, form_title, data_type, entity_status, created_by,
                       created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'pzq_received', 'PZQ CDD Received', 'cdd_drug_allocation', 'integer',
        'ACTIVE', '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'cdd-received-PZQ_', 'integer', 'PZQ Received by CDD',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', false, null, null,
        null, 'Date', '', false, false);
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'pzq_received'),
        (SELECT identifier from entity_tag where tag = 'cdd-received-PZQ_'));
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'cdd-received-PZQ', 'integer', 'PZQ Received by CDD',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true, '#gt_(#metadata,"cdd-received-PZQ_",0)', ARRAY['cdd-received-PZQ_'],
        ARRAY['sum'], 'Date', '#get_(#metadata,"cdd-received-PZQ_")', false, false);


INSERT INTO form_field(identifier, name, display, form_title, data_type, entity_status, created_by,
                       created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'albendazole_received', 'ALB Supervisor Distributed', 'cdd_drug_allocation', 'integer',
        'ACTIVE', '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'cdd-received-ALB_', 'integer', 'ALB Received by CDD',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', false, null, null,
        null, 'Date', '', false, false);
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'albendazole_received'),
        (SELECT identifier from entity_tag where tag = 'cdd-received-ALB_'));
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'cdd-received-ALB', 'integer', 'ALB Received by CDD',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true, '#gt_(#metadata,"cdd-received-ALB_",0)', ARRAY['cdd-received-ALB_'],
        ARRAY['sum'], 'Date', '#get_(#metadata,"cdd-received-ALB_")', false, false);

--drug tablet allocation
INSERT INTO form_field(identifier, name, display, form_title, data_type, entity_status, created_by,
                       created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'drug_distributed', 'drug_distributed', 'tablet_accountability', 'string',
        'ACTIVE', '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'tab-account-mda-lite-drug', 'string', 'MEB Returned to Supervisor',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', false, null, null,
        null, 'Plan', '', false, true);
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'drug_distributed'),
        (SELECT identifier from entity_tag where tag = 'tab-account-mda-lite-drug'));
--tablet account
INSERT INTO form_field(identifier, name, display, form_title, data_type, entity_status, created_by,
                       created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'sum_pzq_received_and_top_up', 'PZQ Supervisor Distributed', 'tablet_accountability', 'integer',
        'ACTIVE', '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'supervisor-distributed-PZQ_', 'integer', 'ALB Supervisor Distributed',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', false, null, null,
        null, 'Plan', '', false, true);
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'sum_pzq_received_and_top_up'),
        (SELECT identifier from entity_tag where tag = 'supervisor-distributed-PZQ_'));
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'supervisor-distributed-PZQ', 'integer', 'PZQ Supervisor Distributed',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true, '#gt_(#metadata,"supervisor-distributed-PZQ_",0) and #eq_(#metadata,"tab-account-mda-lite-drug","Praziquantel (PZQ)")', ARRAY['supervisor-distributed-PZQ_','tab-account-mda-lite-drug'],
        ARRAY['sum'], 'Plan', '#get_(#metadata,"supervisor-distributed-PZQ_")', false, false);

INSERT INTO form_field(identifier, name, display, form_title, data_type, entity_status, created_by,
                       created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'sum_mbz_received_and_top_up', 'MEB Supervisor Distributed', 'tablet_accountability', 'integer',
        'ACTIVE', '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'supervisor-distributed-MEB_', 'integer', 'MEB Supervisor Distributed',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', false, null, null,
        null, 'Plan', '', false, true);
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'sum_mbz_received_and_top_up'),
        (SELECT identifier from entity_tag where tag = 'supervisor-distributed-MEB_'));
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'supervisor-distributed-MEB', 'integer', 'MEB Supervisor Distributed',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true, '#gt_(#metadata,"supervisor-distributed-MEB_",0) and #eq_(#metadata,"tab-account-mda-lite-drug","Mebendazole (MEB)")', ARRAY['supervisor-distributed-MEB_','tab-account-mda-lite-drug'],
        ARRAY['sum'], 'Plan', '#get_(#metadata,"supervisor-distributed-MEB_")', false, false);


INSERT INTO form_field(identifier, name, display, form_title, data_type, entity_status, created_by,
                       created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'sum_alb_received_and_top_up', 'ALB Supervisor Distributed', 'tablet_accountability', 'integer',
        'ACTIVE', '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'supervisor-distributed-ALB_', 'integer', 'ALB Supervisor Distributed',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', false, null, null,
        null, 'Plan', '', false, true);
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'sum_alb_received_and_top_up'),
        (SELECT identifier from entity_tag where tag = 'supervisor-distributed-ALB_'));
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'supervisor-distributed-ALB', 'integer', 'ALB Supervisor Distributed',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true, '#gt_(#metadata,"supervisor-distributed-ALB_",0) and #eq_(#metadata,"tab-account-mda-lite-drug","Albendazole (ALB)")', ARRAY['supervisor-distributed-ALB_'],
        ARRAY['sum'], 'Plan', '#get_(#metadata,"supervisor-distributed-ALB_")', false, false);



INSERT INTO form_field(identifier, name, display, form_title, data_type, entity_status, created_by,
                       created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'albendazole_returned', 'ALB Returned to Supervisor', 'tablet_accountability', 'integer',
        'ACTIVE', '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'supervisor-returned-ALB_', 'integer', 'ALB Returned to Supervisor',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', false, null, null,
        null, 'Plan', '', false, true);
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'albendazole_returned'),
        (SELECT identifier from entity_tag where tag = 'supervisor-returned-ALB_'));
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'supervisor-returned-ALB', 'integer', 'ALB Returned to Supervisor',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true, '#gt_(#metadata,"supervisor-returned-ALB_",0) and #eq_(#metadata,"tab-account-mda-lite-drug","Albendazole (ALB)")', ARRAY['supervisor-returned-ALB_','tab-account-mda-lite-drug'],
        ARRAY['sum'], 'Plan', '#get_(#metadata,"supervisor-returned-ALB_")', false, false);



INSERT INTO form_field(identifier, name, display, form_title, data_type, entity_status, created_by,
                       created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'pzq_returned', 'PZQ Returned to Supervisor', 'tablet_accountability', 'integer',
        'ACTIVE', '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'supervisor-returned-PZQ_', 'integer', 'PZQ Returned to Supervisor',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', false, null, null,
        null, 'Plan', '', false, true);
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'pzq_returned'),
        (SELECT identifier from entity_tag where tag = 'supervisor-returned-PZQ_'));
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'supervisor-returned-PZQ', 'integer', 'PZQ Returned to Supervisor',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true, '#gt_(#metadata,"supervisor-returned-PZQ_",0) and #eq_(#metadata,"tab-account-mda-lite-drug","Praziquantel (PZQ)")', ARRAY['supervisor-returned-PZQ_','tab-account-mda-lite-drug'],
        ARRAY['sum'], 'Plan', '#get_(#metadata,"supervisor-returned-PZQ_")', false, false);



INSERT INTO form_field(identifier, name, display, form_title, data_type, entity_status, created_by,
                       created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'mebendazole_returned', 'MEB Returned to Supervisor', 'tablet_accountability', 'integer',
        'ACTIVE', '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'supervisor-returned-MEB_', 'integer', 'MEB Returned to Supervisor',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', false, null, null,
        null, 'Plan', '', false, true);
INSERT INTO form_field_entity_tag(form_field_identifier, entity_tag_identifier)
VALUES ((SELECT identifier FROM form_field WHERE name = 'mebendazole_returned'),
        (SELECT identifier from entity_tag where tag = 'supervisor-returned-MEB_'));
INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'supervisor-returned-MEB', 'integer', 'MEB Returned to Supervisor',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true, '#gt_(#metadata,"supervisor-returned-MEB_",0) and #eq_(#metadata,"tab-account-mda-lite-drug","Mebendazole (MEB)")', ARRAY['supervisor-returned-MEB_','tab-account-mda-lite-drug'],
        ARRAY['sum'], 'Plan', '#get_(#metadata,"supervisor-returned-MEB_")', false, false);

INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'cdd-remaining-MEB', 'integer', 'MEB Remaining with CDD',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true, '(#gt_(#metadata,"supervisor-distributed-MEB_",0) and #gt_(#metadata,"supervisor-returned-MEB_",-1))', ARRAY['supervisor-returned-MEB_','supervisor-distributed-MEB_'],
        ARRAY['sum'], 'Plan', '((#get_(#metadata,"supervisor-distributed-MEB_")==null?0:#get_(#metadata,"supervisor-distributed-MEB_")) - (#get_(#metadata,"supervisor-returned-MEB_")==null?0:#get_(#metadata,"supervisor-returned-MEB_")))', false, false);

INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'cdd-remaining-PZQ', 'integer', 'PZQ Remaining with CDD',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true, '(#gt_(#metadata,"supervisor-distributed-PZQ_",0) and #gt_(#metadata,"supervisor-returned-PZQ_",-1))', ARRAY['supervisor-returned-PZQ_','supervisor-distributed-PZQ_'],
        ARRAY['sum'], 'Plan', '((#get_(#metadata,"supervisor-distributed-PZQ_")==null?0:#get_(#metadata,"supervisor-distributed-PZQ_")) - (#get_(#metadata,"supervisor-returned-PZQ_")==null?0:#get_(#metadata,"supervisor-returned-PZQ_")))', false, false);


INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'cdd-remaining-ALB', 'integer', 'ALB Remaining with CDD',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', true, '(#gt_(#metadata,"supervisor-distributed-ALB_",0) and #gt_(#metadata,"supervisor-returned-ALB_",-1))', ARRAY['supervisor-returned-ALB_','supervisor-distributed-ALB_'],
        ARRAY['sum'], 'Plan', '((#get_(#metadata,"supervisor-distributed-ALB_")==null?0:#get_(#metadata,"supervisor-distributed-ALB_")) - (#get_(#metadata,"supervisor-returned-ALB_")==null?0:#get_(#metadata,"supervisor-returned-ALB_")))', false, false);


INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'sth-target-pop', 'integer', 'STH Target Population',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', false, null, null,
        null, 'Plan', null, false, false);

INSERT INTO entity_tag(identifier, tag, value_type, definition, lookup_entity_type_identifier,
                       entity_status, created_by, created_datetime, modified_by, modified_datetime,
                       generated, generation_formula, referenced_fields, aggregation_method, scope,
                       result_expression, is_result_literal, add_to_metadata)
VALUES (uuid_generate_v4(), 'sch-target-pop', 'integer', 'SCH Target Population',
        (SELECT identifier from lookup_entity_type where code = 'Location'), 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00', false, null, null,
        null, 'Plan', null, false, false);

