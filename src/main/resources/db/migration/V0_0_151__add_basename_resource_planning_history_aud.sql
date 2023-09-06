ALTER TABLE IF EXISTS resource_planning_history_aud
    ADD COLUMN IF NOT EXISTS  base_name character varying NOT NULL DEFAULT '';

