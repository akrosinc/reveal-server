
ALTER TABLE  IF EXISTS task
    ADD COLUMN  IF NOT EXISTS parent_task_identifier uuid;

ALTER TABLE  IF EXISTS task_aud
    ADD COLUMN  IF NOT EXISTS parent_task_identifier uuid;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_task_parent'
    ) THEN
ALTER TABLE IF EXISTS task
    ADD CONSTRAINT fk_task_parent
        FOREIGN KEY (parent_task_identifier)
            REFERENCES task(identifier);
END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_task_parent_task
    ON task(parent_task_identifier);
