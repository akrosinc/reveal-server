
ALTER TABLE IF EXISTS  "group_aud"
    ADD COLUMN entity_status character varying(36) NOT NULL;

ALTER TABLE IF EXISTS  person_aud
    ADD COLUMN entity_status character varying(36) NOT NULL;