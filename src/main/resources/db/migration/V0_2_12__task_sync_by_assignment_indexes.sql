-- Supports task sync driven by plan_assignment (selected locations + all descendants).
-- Descendants are resolved through the materialised ancestry path on location_relationship.

CREATE INDEX IF NOT EXISTS idx_location_relationship_ancestry_gin
    ON location_relationship USING gin (ancestry);

CREATE INDEX IF NOT EXISTS idx_task_location_location
    ON task_location (location_identifier);

CREATE INDEX IF NOT EXISTS idx_task_location_task
    ON task_location (task_identifier);

CREATE INDEX IF NOT EXISTS idx_task_person_task
    ON task_person (task_identifier);

CREATE INDEX IF NOT EXISTS idx_person_location_location
    ON person_location (location_identifier);

CREATE INDEX IF NOT EXISTS idx_person_location_person
    ON person_location (person_identifier);

CREATE INDEX IF NOT EXISTS idx_task_plan_server_version
    ON task (plan_identifier, server_version);

CREATE INDEX IF NOT EXISTS idx_plan_locations_plan
    ON plan_locations (plan_identifier);

CREATE INDEX IF NOT EXISTS idx_plan_assignment_org_selected
    ON plan_assignment (organization_identifier) WHERE selected = true;

CREATE INDEX IF NOT EXISTS idx_plan_assignment_plan_locations
    ON plan_assignment (plan_locations_identifier);
