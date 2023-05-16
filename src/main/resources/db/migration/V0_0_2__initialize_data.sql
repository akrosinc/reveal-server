INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES ('21c15586-a002-4655-993c-574e8ea3d9e1', 'country', 'Country', 'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00', '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING ;

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES ('196b31f3-b9f3-4edc-8ec2-b92dbd6fd606', 'province', 'Province', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING ;

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES ('1af5f5f1-61eb-4e53-99b8-c28a8a1f657a', 'district', 'District', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING ;

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES ('785dbbe9-baad-4243-8581-52229e05af99', 'operational', 'Operational', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING ;

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES ('59a60fc2-d035-40ed-ba66-a2157d45686c', 'catchment', 'Catchment', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING ;

INSERT INTO geographic_level(identifier, name, title, entity_status, created_by, created_datetime,
                             modified_by, modified_datetime)
VALUES ('74692544-7bff-49d4-948c-6f0805a726e1', 'structure', 'Structure', 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING ;


INSERT INTO location_hierarchy(identifier, node_order, name, entity_status, created_by,
                               created_datetime, modified_by, modified_datetime)
VALUES ( 'dc36e4fe-4218-4416-8b74-2d5e2eb09e62',
         ARRAY [
             (SELECT name FROM geographic_level where name = 'country'),
             (SELECT name FROM geographic_level where name = 'province'),
             (SELECT name FROM geographic_level where name = 'district'),
             (SELECT name FROM geographic_level where name = 'catchment'),
             (SELECT name FROM geographic_level where name = 'operational'),
             (SELECT name FROM geographic_level where name = 'structure')
             ]
       , 'default'
       , 'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00'
       , '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00')  ON CONFLICT DO NOTHING ;


INSERT INTO lookup_intervention_type(identifier, name, code, entity_status, created_by,
                                     created_datetime, modified_by, modified_datetime)
VALUES ('730d58d4-8ca8-4ef0-93cb-4cc9a7ec654f', 'IRS', 'IRS',
        'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00')  ON CONFLICT DO NOTHING ;

INSERT INTO lookup_intervention_type(identifier, name, code, entity_status, created_by,
                                     created_datetime, modified_by, modified_datetime)
VALUES ('a3b5869e-20af-43fa-8611-983e0c9047d1', 'MDA', 'MDA',
        'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING ;

INSERT INTO form(identifier, name, title, template, payload, entity_status, created_by,
                 created_datetime, modified_by, modified_datetime)
VALUES ('8d57ccc9-5ca4-4b85-a853-1f0d4b7d4b14', 'FormVisitStructures', 'FormVisitStructures', false, '{}',
        'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING ;

INSERT INTO form(identifier, name, title, template, payload, entity_status, created_by,
                 created_datetime, modified_by, modified_datetime)
VALUES ('99a226ea-6dc1-4acd-a6a1-1f4d07b0595f', 'FormRegisterPeople', 'FormRegisterPeople', false, '{}',
        'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING ;

INSERT INTO form(identifier, name, title, template, payload, entity_status, created_by,
                 created_datetime, modified_by, modified_datetime)
VALUES ('80181ff7-5e53-4f72-994d-d106a4c83e66', 'FormDispenseDrugs', 'FormDispenseDrugs', false, '{}',
        'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING ;

INSERT INTO organization(identifier, active, name, type, organization_parent_id, entity_status,
                         created_by, created_datetime, modified_by, modified_datetime)
VALUES ('19c9a023-fff3-4aa8-a355-999effadc4cc', true, 'Product Team', 'CG', null, 'ACTIVE',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00',
        '71fca736-c156-40bc-9de5-3ae04981fbc9', '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING ;


INSERT INTO users(identifier, sid, username, first_name, last_name, email, api_response,
                         user_bulk_identifier, entity_status, created_by, created_datetime,
                         modified_by, modified_datetime)
VALUES ('71fca736-c156-40bc-9de5-3ae04981fbc9', '71fca736-c156-40bc-9de5-3ae04981fbc9', 'test.user',
        'Test', 'User', null, null, null, 'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00', '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING ;

INSERT INTO users(identifier, sid, username, first_name, last_name, email, api_response,
                         user_bulk_identifier, entity_status, created_by, created_datetime,
                         modified_by, modified_datetime)
VALUES ('52264802-36d9-49ac-ac8e-b285d03d5aea', '52264802-36d9-49ac-ac8e-b285d03d5aea', 'demo.user',
        'Demo', 'User', null, null, null, 'ACTIVE', '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00', '71fca736-c156-40bc-9de5-3ae04981fbc9',
        '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING ;

INSERT INTO lookup_task_status(identifier, name, code, entity_status, created_by, created_datetime,
                               modified_by, modified_datetime)
VALUES ('b9734e70-c8e2-4360-b9bf-ce08275a95a5', 'READY', 'READY', 'ACTIVE', '649f338b-eb53-4832-9562-f695e9cc44e7',
        '2022-01-12 13:54:22.106221+00', '649f338b-eb53-4832-9562-f695e9cc44e7',
        '2022-01-12 13:54:22.106221+00'),
       ('e175ffa6-041a-48f3-b685-2be06d90e4f1', 'COMPLETED', 'COMPLETED', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00'),
       ('49f89922-04a5-4cf8-aa6a-b6ba0ff60fc9', 'CANCELLED', 'CANCELLED', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00'),
       ('feac6c5c-a39e-4e1b-81e0-5d471e996af7', 'DRAFT', 'DRAFT', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00'),
       ('0ec322f2-f263-4a48-98ff-f3b2e74b6321', 'IN_PROGRESS', 'IN_PROGRESS', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00'),
       ('fe61836a-4c56-4180-bf20-89ac043e09b0', 'ARCHIVED', 'ARCHIVED', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00'),
       ('7fd99cdb-f4f0-4ca1-9c38-82456c5ffc1b', 'FAILED', 'FAILED', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING ;

INSERT INTO lookup_entity_type(identifier, code, table_name, entity_status, created_by,
                               created_datetime, modified_by, modified_datetime)
VALUES ('cf940bb8-521a-4431-a43b-b95a20d2d6d4', 'Person', 'person', 'ACTIVE', '649f338b-eb53-4832-9562-f695e9cc44e7',
        '2022-01-12 13:54:22.106221+00', '649f338b-eb53-4832-9562-f695e9cc44e7',
        '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING ;

INSERT INTO lookup_entity_type(identifier, code, table_name, entity_status, created_by,
                               created_datetime, modified_by, modified_datetime)
VALUES ('99012adf-18aa-4521-84c2-d0c2be179f55', 'Location', 'location', 'ACTIVE',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00',
        '649f338b-eb53-4832-9562-f695e9cc44e7', '2022-01-12 13:54:22.106221+00') ON CONFLICT DO NOTHING ;

INSERT INTO entity_tag(identifier, tag, value_type, lookup_entity_type_identifier, entity_status,
                       created_by, created_datetime, modified_by, modified_datetime)
select 'd21fd939-5389-43a3-8792-381f8ecfa371',
       'business-status',
       'string',
       identifier,
       'ACTIVE',
       '649f338b-eb53-4832-9562-f695e9cc44e7',
       '2022-01-12 13:54:22.106221+00',
       '649f338b-eb53-4832-9562-f695e9cc44e7',
       '2022-01-12 13:54:22.106221+00'
from lookup_entity_type
where code = 'Location' ON CONFLICT DO NOTHING ;

INSERT INTO entity_tag(identifier, tag, value_type, lookup_entity_type_identifier, entity_status,
                       created_by, created_datetime, modified_by, modified_datetime)
select 'c480af2b-6195-4075-b5b1-801a55b8a439',
       'business-status',
       'string',
       identifier,
       'ACTIVE',
       '649f338b-eb53-4832-9562-f695e9cc44e7',
       '2022-01-12 13:54:22.106221+00',
       '649f338b-eb53-4832-9562-f695e9cc44e7',
       '2022-01-12 13:54:22.106221+00'
from lookup_entity_type
where code = 'Person' ON CONFLICT DO NOTHING ;
