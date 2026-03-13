ALTER TABLE IF EXISTS organization_aud
    ADD COLUMN IF NOT EXISTS instance_identifier UUID;