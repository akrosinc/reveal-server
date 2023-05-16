ALTER TABLE IF EXISTS entity_tag
    ADD COLUMN IF NOT EXISTS is_aggregate boolean NOT NULL DEFAULT false;

ALTER TABLE IF EXISTS entity_tag_aud
    ADD COLUMN IF NOT EXISTS is_aggregate boolean NOT NULL DEFAULT false;
