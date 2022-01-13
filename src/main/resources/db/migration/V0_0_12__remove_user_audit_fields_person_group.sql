ALTER TABLE IF EXISTS person_group DROP COLUMN IF EXISTS created_by;
ALTER TABLE IF EXISTS person_group DROP COLUMN IF EXISTS created_datetime;
ALTER TABLE IF EXISTS person_group DROP COLUMN IF EXISTS modified_by;
ALTER TABLE IF EXISTS person_group DROP COLUMN IF EXISTS modified_datetime;

ALTER TABLE IF EXISTS person_group_aud DROP COLUMN IF EXISTS created_by;
ALTER TABLE IF EXISTS person_group_aud DROP COLUMN IF EXISTS created_datetime;
ALTER TABLE IF EXISTS person_group_aud DROP COLUMN IF EXISTS modified_by;
ALTER TABLE IF EXISTS person_group_aud DROP COLUMN IF EXISTS modified_datetime;