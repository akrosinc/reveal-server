ALTER TABLE IF EXISTS task
    ADD COLUMN task_facade jsonb;

ALTER TABLE IF EXISTS task_aud
    ADD COLUMN task_facade jsonb;