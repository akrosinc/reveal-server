ALTER TABLE event
ALTER COLUMN task_identifier DROP NOT NULL,
ALTER COLUMN base_entity_identifier DROP NOT NULL;

ALTER TABLE event_aud
ALTER COLUMN task_identifier DROP NOT NULL,
ALTER COLUMN base_entity_identifier DROP NOT NULL;
