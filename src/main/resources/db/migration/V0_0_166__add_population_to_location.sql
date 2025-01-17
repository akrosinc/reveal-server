ALTER TABLE IF EXISTS location
ADD COLUMN population_data JSONB;

ALTER TABLE IF EXISTS location_aud
ADD COLUMN population_data JSONB;