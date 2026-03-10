ALTER TABLE IF EXISTS location_hierarchy
    ADD COLUMN IF NOT EXISTS
            is_base_hierarchy BOOLEAN NOT NULL DEFAULT FALSE;