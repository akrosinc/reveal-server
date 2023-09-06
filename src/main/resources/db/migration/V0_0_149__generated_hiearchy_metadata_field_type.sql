ALTER TABLE IF EXISTS generated_hierarchy_metadata
    ADD COLUMN IF NOT EXISTS field_type character varying;