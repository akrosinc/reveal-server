DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_permission_name'
    ) THEN
ALTER TABLE permissions
    ADD CONSTRAINT uq_permission_name UNIQUE (name);
END IF;
END $$;
