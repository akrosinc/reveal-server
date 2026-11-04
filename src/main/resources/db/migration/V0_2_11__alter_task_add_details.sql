-- Add a jsonb `details` column to the task table to store extensible task metadata.
-- Initial contents: { "limitCounter": <int> } indicating the number of times a
-- task can be executed before it is closed.

ALTER TABLE IF EXISTS task
    ADD COLUMN IF NOT EXISTS details jsonb;

ALTER TABLE IF EXISTS task_aud
    ADD COLUMN IF NOT EXISTS details jsonb;
