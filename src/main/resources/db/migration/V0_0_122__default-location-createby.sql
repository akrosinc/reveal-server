ALTER TABLE IF EXISTS location
    ALTER COLUMN created_by SET DEFAULT 'default_user';

ALTER TABLE IF EXISTS location
    ALTER COLUMN modified_by SET DEFAULT 'default_user';

ALTER TABLE IF EXISTS location_aud
    ALTER COLUMN created_by SET DEFAULT 'default_user';

ALTER TABLE IF EXISTS location_aud
    ALTER COLUMN modified_by SET DEFAULT 'default_user';