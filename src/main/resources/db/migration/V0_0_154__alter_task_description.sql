ALTER TABLE IF EXISTS task
ALTER COLUMN  description DROP NOT NULL;

ALTER TABLE IF EXISTS task_aud
ALTER COLUMN  description DROP NOT NULL;