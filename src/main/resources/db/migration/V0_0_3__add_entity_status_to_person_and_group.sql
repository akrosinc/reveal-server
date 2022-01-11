
ALTER TABLE IF EXISTS  "group"
    ADD COLUMN entity_status character varying(36) NOT NULL;

ALTER TABLE IF EXISTS  person
    ADD COLUMN entity_status character varying(36) NOT NULL;
