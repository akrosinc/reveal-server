ALTER TABLE IF EXISTS task
    ADD COLUMN IF NOT EXISTS task_facade jsonb;

ALTER TABLE IF EXISTS task_aud
    ADD COLUMN IF NOT EXISTS task_facade jsonb;