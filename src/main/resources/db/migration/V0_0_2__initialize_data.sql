INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'country', 'Country', 'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00', '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00');

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'province', 'Province', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00');

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'district', 'District', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00');

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'operational', 'Operational', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00');

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'structure', 'Structure', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00');


INSERT INTO location_hierarchy(identifier, node_order, name, entity_status, created_by,
                               created_datetime, modified_by, modified_datetime)
VALUES ( uuid_generate_v4(),
         ARRAY [
                 (SELECT name FROM geographic_level where name = 'country'),
                 (SELECT name FROM geographic_level where name = 'province'),
                 (SELECT name FROM geographic_level where name = 'district'),
             (SELECT name FROM geographic_level where name = 'operational'),
                 (SELECT name FROM geographic_level where name = 'structure')
             ]
       , 'IRS'
       , 'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00'
       , '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00');


INSERT INTO lookup_intervention_type(identifier, name, code, entity_status, created_by,
                                     created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'IRS', 'IRS',
        'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00');


INSERT INTO form(identifier, name, title, template, payload, entity_status, created_by,
                 created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'FormVisitStructures', 'FormVisitStructures', false, '{}',
        'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00');

INSERT INTO form(identifier, name, title, template, payload, entity_status, created_by,
                 created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'FormRegisterPeople', 'FormRegisterPeople', false, '{}',
        'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00');

INSERT INTO form(identifier, name, title, template, payload, entity_status, created_by,
                 created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'FormDispenseDrugs', 'FormDispenseDrugs', false, '{}',
        'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00');

INSERT INTO organization(identifier, active, name, type, organization_parent_id, entity_status,
                         created_by, created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), true, 'TeamOne', 'CG', null, 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00');

INSERT INTO organization(identifier, active, name, type, organization_parent_id, entity_status,
                         created_by, created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), true, 'TeamTwo', 'CG', null, 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00');

INSERT INTO organization(identifier, active, name, type, organization_parent_id, entity_status,
                         created_by, created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), true, 'TeamOneOne', 'CG',
        (SELECT identifier From organization WHERE name = 'TeamOne'), 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00');

INSERT INTO public.users(identifier, sid, username, first_name, last_name, email, api_response,
                         user_bulk_identifier, entity_status, created_by, created_datetime,
                         modified_by, modified_datetime)
VALUES ('71fca736-c156-40bc-9de5-3ae04981fbc9', '71fca736-c156-40bc-9de5-3ae04981fbc9', 'test.user',
        'Test', 'User', null, null, null, 'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00', '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00');

INSERT INTO lookup_task_status(identifier, name, code, entity_status, created_by, created_datetime,
                               modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'READY', 'READY', 'ACTIVE', '649f338b-eb53-4832-9562-f695e9cc44e7',
        '2022-01-12 13:54:22.106221+00', '649f338b-eb53-4832-9562-f695e9cc44e7',
        '2022-01-12 13:54:22.106221+00'),
       (uuid_generate_v4(), 'COMPLETED', 'COMPLETED', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00'),
       (uuid_generate_v4(), 'CANCELLED', 'CANCELLED', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00'),
       (uuid_generate_v4(), 'DRAFT', 'DRAFT', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00'),
       (uuid_generate_v4(), 'IN_PROGRESS', 'IN_PROGRESS', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00'),
       (uuid_generate_v4(), 'ARCHIVED', 'ARCHIVED', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00'),
        (uuid_generate_v4(), 'FAILED', 'FAILED', 'ACTIVE',
            '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
            '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');

INSERT INTO lookup_entity_type(identifier, code, table_name, entity_status, created_by,
                               created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'Person', 'person', 'ACTIVE', '649f338b-eb53-4832-9562-f695e9cc44e7',
        '2022-01-12 13:54:22.106221+00', '649f338b-eb53-4832-9562-f695e9cc44e7',
        '2022-01-12 13:54:22.106221+00');

INSERT INTO lookup_entity_type(identifier, code, table_name, entity_status, created_by,
                               created_datetime, modified_by, modified_datetime)
VALUES (uuid_generate_v4(), 'Location', 'location', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00');

INSERT INTO entity_tag(identifier, tag, value_type, lookup_entity_type_identifier, entity_status,
                       created_by, created_datetime, modified_by, modified_datetime)
select uuid_generate_v4(),
       'business_status',
       'string',
       identifier,
       'ACTIVE',
       '649f338b-eb53-4832-9562-f695e9cc44e7',
       '2022-01-12 13:54:22.106221+00',
       '649f338b-eb53-4832-9562-f695e9cc44e7',
       '2022-01-12 13:54:22.106221+00'
from lookup_entity_type
where code = 'Location';

INSERT INTO entity_tag(identifier, tag, value_type, lookup_entity_type_identifier, entity_status,
                       created_by, created_datetime, modified_by, modified_datetime)
select uuid_generate_v4(),
       'business_status',
       'string',
       identifier,
       'ACTIVE',
       '649f338b-eb53-4832-9562-f695e9cc44e7',
       '2022-01-12 13:54:22.106221+00',
       '649f338b-eb53-4832-9562-f695e9cc44e7',
       '2022-01-12 13:54:22.106221+00'
from lookup_entity_type
where code = 'Person';
