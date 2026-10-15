
ALTER TABLE  IF EXISTS "action"
    ADD COLUMN  IF NOT EXISTS config jsonb;

ALTER TABLE  IF EXISTS action_aud
    ADD COLUMN  IF NOT EXISTS config jsonb;
