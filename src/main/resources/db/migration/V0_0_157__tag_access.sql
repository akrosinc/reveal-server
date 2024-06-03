alter table entity_tag
    add column if not exists metadata_import_id uuid;

alter table entity_tag
    add column if not exists referenced_tag uuid;
