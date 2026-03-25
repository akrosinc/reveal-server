-- Add new column with default value
ALTER TABLE IF EXISTS location_hierarchy
    ADD COLUMN IF NOT EXISTS hierarchy_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- Add column to audit table
ALTER TABLE IF EXISTS location_hierarchy_aud
    ADD COLUMN IF NOT EXISTS hierarchy_status VARCHAR(50);