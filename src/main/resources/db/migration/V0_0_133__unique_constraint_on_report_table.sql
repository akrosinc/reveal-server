ALTER TABLE report
ADD CONSTRAINT location_per_plan_unique UNIQUE (plan_id, location_id);
