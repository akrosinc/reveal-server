
ALTER TABLE  IF EXISTS task_process_stage
    ADD COLUMN  IF NOT EXISTS location_identifier uuid;

ALTER TABLE  IF EXISTS task_process_stage_aud
    ADD COLUMN  IF NOT EXISTS location_identifier uuid;

