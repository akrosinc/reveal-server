ALTER TABLE location
ADD COLUMN IF NOT EXISTS location_property jsonb;

ALTER TABLE location_aud
ADD COLUMN IF NOT EXISTS location_property jsonb;