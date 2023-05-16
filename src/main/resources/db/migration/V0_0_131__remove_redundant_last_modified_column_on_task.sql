alter table IF EXISTS task
    drop column IF EXISTS last_modified;

alter table IF EXISTS task_aud
    drop column IF EXISTS last_modified;


