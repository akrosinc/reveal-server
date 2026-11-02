-- Add instance to cost_by_location and prevent duplicate cost/recapture rows per instance+location+action

ALTER TABLE IF EXISTS cost_by_location
    ADD COLUMN IF NOT EXISTS instance_identifier uuid;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cost_by_location_instance') THEN
        ALTER TABLE cost_by_location
            ADD CONSTRAINT fk_cost_by_location_instance
            FOREIGN KEY (instance_identifier)
            REFERENCES instance (identifier);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_cost_by_location_instance_location_action') THEN
        ALTER TABLE cost_by_location
            ADD CONSTRAINT uq_cost_by_location_instance_location_action
            UNIQUE (instance_identifier, location_identifier, action_identifier);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_recapture_limit_instance_location_action') THEN
        ALTER TABLE action_location_recapture_limit
            ADD CONSTRAINT uq_recapture_limit_instance_location_action
            UNIQUE (instance_identifier, location_identifier, action_identifier);
    END IF;
END $$;
