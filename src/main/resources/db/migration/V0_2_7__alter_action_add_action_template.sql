ALTER TABLE IF EXISTS "action"
    ADD COLUMN IF NOT EXISTS action_template_identifier uuid;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_action_action_template') THEN
        ALTER TABLE "action"
            ADD CONSTRAINT fk_action_action_template
            FOREIGN KEY (action_template_identifier)
            REFERENCES action_template (identifier);
    END IF;
END $$;

ALTER TABLE IF EXISTS action_aud
    ADD COLUMN IF NOT EXISTS action_template_identifier uuid;
