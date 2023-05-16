ALTER TABLE entity_tag
    add column IF NOT EXISTS simulation_display boolean default false;
ALTER TABLE entity_tag_aud
    add column IF NOT EXISTS simulation_display boolean default false;