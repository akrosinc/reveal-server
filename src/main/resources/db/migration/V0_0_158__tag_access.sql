ALTER TABLE IF EXISTS entity_tag_ownership ADD UNIQUE  (entity_tag_id,user_sid);


CREATE TABLE IF NOT EXISTS metadata_import_ownership
(
    id            uuid not null,
    metadata_import_id uuid not null,
    user_sid      uuid not null,
    primary key (id)
    );
CREATE INDEX IF NOT EXISTS idx_metadata_import_ownership_metadata_import_id on metadata_import_ownership (user_sid);
CREATE INDEX IF NOT EXISTS idx_metadata_import_ownership_user_sid on metadata_import_ownership (user_sid);

ALTER TABLE IF EXISTS metadata_import_ownership ADD UNIQUE  (metadata_import_id,user_sid);


CREATE TABLE IF NOT EXISTS complex_tag_ownership
(
    id            uuid not null,
    complex_tag_id int not null,
    user_sid      uuid not null,
    primary key (id)
    );
CREATE INDEX IF NOT EXISTS idx_complex_tag_ownership_user_sid on complex_tag_ownership (user_sid);
CREATE INDEX IF NOT EXISTS idx_complex_tag_ownership_complex_tag_id on complex_tag_ownership (complex_tag_id);

ALTER TABLE IF EXISTS complex_tag_ownership ADD UNIQUE  (complex_tag_id,user_sid);


ALTER TABLE IF EXISTS entity_tag ADD COLUMN IF NOT EXISTS is_deleting boolean default false;
