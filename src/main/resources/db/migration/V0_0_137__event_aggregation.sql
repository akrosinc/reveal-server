CREATE OR REPLACE FUNCTION is_date(
    s character varying)
    RETURNS boolean
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS
$BODY$
begin
    perform s::date;
    return true;
exception
    when others then
        return false;
end;
$BODY$;

CREATE OR REPLACE FUNCTION is_jsonb(
    s character varying)
    RETURNS boolean
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS
$BODY$
begin
    perform s::jsonb;
    return true;
exception
    when others then
        return false;
end;
$BODY$;

CREATE OR REPLACE FUNCTION is_uuid(
    s character varying)
    RETURNS boolean
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS
$BODY$
begin
    perform s::uuid;
    return true;
exception
    when others then
        return false;
end;
$BODY$;

CREATE SEQUENCE IF NOT EXISTS event_aggregation_numeric_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS event_aggregation_string_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;


CREATE MATERIALIZED VIEW IF NOT EXISTS event_max_server_version
AS
SELECT eve.location_identifier,
       lr.ancestry,
       eve.plan_identifier,
       eve.event_type,
       eve.fieldcode,
       eve.val ~ '^[0-9\.]+$'::text         AS isnumber,
       is_date(eve.val::character varying)  AS isdate,
       is_uuid(eve.val::character varying)  AS isuuid,
       is_jsonb(eve.val::character varying) AS isjsonb,
       eve.val
FROM (SELECT COALESCE(e.base_entity_identifier, e.location_identifier) AS location_identifier,
             e.plan_identifier,
             e.event_type,
             arr.obj ->> 'fieldCode'::text                             AS fieldcode,
             (arr.obj -> 'values'::text) ->> 0                         AS val
      FROM (SELECT e_1.identifier,
                   e_1.server_version,
                   e_1.event_type,
                   e_1.user_identifier,
                   e_1.capture_datetime,
                   e_1.plan_identifier,
                   e_1.organization_identifier,
                   e_1.location_identifier,
                   e_1.additional_information,
                   e_1.details,
                   e_1.task_identifier,
                   e_1.base_entity_identifier,
                   e_1.entity_status,
                   e_1.created_by,
                   e_1.created_datetime,
                   e_1.modified_by,
                   e_1.modified_datetime
            FROM (SELECT e_2.plan_identifier,
                         e_2.base_entity_identifier,
                         e_2.event_type,
                         max(e_2.server_version) AS max_version
                  FROM event e_2
                  GROUP BY e_2.plan_identifier, e_2.base_entity_identifier, e_2.event_type) eve_1
                     JOIN event e_1 ON e_1.server_version = eve_1.max_version) e,
           LATERAL jsonb_array_elements(e.additional_information -> 'obs'::text) WITH ORDINALITY arr(obj, pos)
      WHERE (arr.obj ->> 'fieldCode'::text) !~~ '%AAAAAAAAAAAAA%'::text
        AND (arr.obj ->> 'fieldCode'::text) !~~ 'serial_number%'::text) eve
         JOIN location_relationship lr ON lr.location_identifier = eve.location_identifier
WITH DATA;

CREATE MATERIALIZED VIEW IF NOT EXISTS event_aggregation_string_count
AS
SELECT nextval('event_aggregation_string_seq'::regclass) AS id,
       l.name,
       eve.ancestor,
       eve.plan_identifier,
       eve.event_type,
       eve.fieldcode,
       eve.val
FROM (SELECT eve_1.location_identifier,
             eve_1.ancestry,
             arr.ancestor,
             eve_1.plan_identifier,
             eve_1.event_type,
             eve_1.fieldcode,
             eve_1.val
      FROM (SELECT eve_2.location_identifier,
                   eve_2.ancestry,
                   eve_2.plan_identifier,
                   eve_2.event_type,
                   eve_2.fieldcode,
                   eve_2.val ~ '^[0-9\.]+$'::text AS isnumber,
                   eve_2.val,
                   eve_2.isuuid,
                   eve_2.isjsonb,
                   eve_2.isdate
            FROM event_max_server_version eve_2) eve_1,
           LATERAL unnest(array_append(eve_1.ancestry, eve_1.location_identifier)) WITH ORDINALITY arr(ancestor, pos)
      WHERE eve_1.isuuid = false
        AND eve_1.isjsonb = false
        AND eve_1.isdate = false) eve
         LEFT JOIN location l ON eve.ancestor = l.identifier
WITH DATA;

CREATE INDEX IF NOT EXISTS  event_aggregation_string_count_ancestor_idx
    ON event_aggregation_string_count USING btree
        (ancestor);


CREATE MATERIALIZED VIEW IF NOT EXISTS event_aggregation_numeric
AS
SELECT nextval('event_aggregation_numeric_seq'::regclass) AS id,
       l.name,
       eve.ancestor,
       eve.plan_identifier,
       eve.event_type,
       eve.fieldcode,
       eve.val
FROM (SELECT eve_1.location_identifier,
             eve_1.ancestry,
             arr.ancestor,
             eve_1.plan_identifier,
             eve_1.event_type,
             eve_1.fieldcode,
             eve_1.val
      FROM event_max_server_version eve_1,
           LATERAL unnest(array_append(eve_1.ancestry, eve_1.location_identifier)) WITH ORDINALITY arr(ancestor, pos)
      WHERE eve_1.isnumber = true) eve
         LEFT JOIN location l ON eve.ancestor = l.identifier
WITH DATA;


CREATE INDEX IF NOT EXISTS event_aggregation_numeric_ancestor_idx
    ON event_aggregation_numeric USING btree
        (ancestor);