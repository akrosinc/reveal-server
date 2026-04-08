ALTER TABLE IF EXISTS simulation_request
ADD COLUMN IF NOT EXISTS dataset_request JSONB;
