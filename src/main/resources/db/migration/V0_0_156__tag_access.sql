CREATE TABLE IF NOT EXISTS entity_tag_ownership
(
    id            uuid not null,
    entity_tag_id uuid not null,
    user_sid      uuid not null,
    primary key (id)
    );
CREATE INDEX IF NOT EXISTS idx_entity_tag_ownership_user_sid on entity_tag_ownership (user_sid);
CREATE INDEX IF NOT EXISTS idx_entity_tag_entity_tag_id on entity_tag_ownership (entity_tag_id);


CREATE TABLE IF NOT EXISTS entity_tag_acc_grants_organization
(
    id            uuid not null,
    entity_tag_id uuid not null,
    organization_id      uuid not null,
    primary key (id)
    );
CREATE INDEX IF NOT EXISTS idx_entity_tag_ownership_organization_id on entity_tag_acc_grants_organization (organization_id);
CREATE INDEX IF NOT EXISTS idx_entity_tag_acc_org_entity_tag_id on entity_tag_acc_grants_organization (entity_tag_id);

CREATE TABLE IF NOT EXISTS entity_tag_acc_grants_user
(
    id            uuid not null,
    entity_tag_id uuid not null,
    user_sid      uuid not null,
    primary key (id)
    );
CREATE INDEX IF NOT EXISTS idx_entity_tag_ownership_organization_id on entity_tag_acc_grants_user (user_sid);
CREATE INDEX IF NOT EXISTS idx_entity_tag_acc_usr_entity_tag_id on entity_tag_acc_grants_user (entity_tag_id);

alter table IF EXISTS entity_tag
    add if not exists is_public boolean DEFAULT false;

ALTER TABLE entity_tag_acc_grants_user ADD UNIQUE (entity_tag_id,user_sid);
ALTER TABLE entity_tag_acc_grants_organization ADD UNIQUE (entity_tag_id,organization_id);

--Complex tag
CREATE TABLE IF NOT EXISTS complex_tag_acc_grants_organization
(
    id            uuid not null,
    complex_tag_id int not null,
    organization_id      uuid not null,
    primary key (id)
    );
CREATE INDEX IF NOT EXISTS idx_complex_tag_ownership_organization_id on complex_tag_acc_grants_organization (organization_id);
CREATE INDEX IF NOT EXISTS idx_complex_tag_acc_org_entity_tag_id on complex_tag_acc_grants_organization (complex_tag_id);

CREATE TABLE IF NOT EXISTS complex_tag_acc_grants_user
(
    id            uuid not null,
    complex_tag_id int not null,
    user_sid      uuid not null,
    primary key (id)
    );
CREATE INDEX IF NOT EXISTS idx_complex_tag_ownership_organization_id on complex_tag_acc_grants_user (user_sid);
CREATE INDEX IF NOT EXISTS idx_complex_tag_acc_usr_entity_tag_id on complex_tag_acc_grants_user (complex_tag_id);

alter table IF EXISTS complex_tag
    add if not exists is_public boolean DEFAULT false;

ALTER TABLE IF EXISTS complex_tag_acc_grants_user ADD UNIQUE (complex_tag_id,user_sid);
ALTER TABLE IF EXISTS complex_tag_acc_grants_organization ADD UNIQUE (complex_tag_id,organization_id);
