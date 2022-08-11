ALTER TABLE IF EXISTS entity_tag
    ADD COLUMN is_aggregate boolean NOT NULL DEFAULT false;

ALTER TABLE IF EXISTS entity_tag_aud
    ADD COLUMN is_aggregate boolean NOT NULL DEFAULT false;
