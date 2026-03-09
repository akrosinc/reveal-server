-- INSTANCE
CREATE TABLE IF NOT EXISTS instance
(
    identifier UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    hierarchy_identifier UUID,

    entity_status VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
                                   modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,

                                   PRIMARY KEY (identifier)
    );

ALTER TABLE instance
DROP CONSTRAINT IF EXISTS fk_instance_hierarchy;

ALTER TABLE instance
    ADD CONSTRAINT fk_instance_hierarchy
        FOREIGN KEY (hierarchy_identifier)
            REFERENCES location_hierarchy(identifier);



-- INSTANCE ROLE
CREATE TABLE IF NOT EXISTS instance_role
(
    identifier UUID PRIMARY KEY,
    name VARCHAR(255)
    );



-- PERMISSIONS
CREATE TABLE IF NOT EXISTS permissions
(
    identifier UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (identifier)
    );



-- INSTANCE ROLE PERMISSION
CREATE TABLE IF NOT EXISTS instance_role_permission
(
    instance_role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (instance_role_id, permission_id)
    );

ALTER TABLE instance_role_permission
DROP CONSTRAINT IF EXISTS fk_irp_role;

ALTER TABLE instance_role_permission
    ADD CONSTRAINT fk_irp_role
        FOREIGN KEY (instance_role_id)
            REFERENCES instance_role(identifier)
            ON DELETE CASCADE;

ALTER TABLE instance_role_permission
DROP CONSTRAINT IF EXISTS fk_irp_permission;

ALTER TABLE instance_role_permission
    ADD CONSTRAINT fk_irp_permission
        FOREIGN KEY (permission_id)
            REFERENCES permissions(identifier)
            ON DELETE CASCADE;



-- INSTANCE USER
CREATE TABLE IF NOT EXISTS instance_user
(
    instance_id UUID NOT NULL,
    user_id UUID NOT NULL,
    instance_role_id UUID,
    PRIMARY KEY (instance_id, user_id)
    );

ALTER TABLE instance_user
DROP CONSTRAINT IF EXISTS fk_instance_user_instance;

ALTER TABLE instance_user
    ADD CONSTRAINT fk_instance_user_instance
        FOREIGN KEY (instance_id)
            REFERENCES instance(identifier)
            ON DELETE CASCADE;

ALTER TABLE instance_user
DROP CONSTRAINT IF EXISTS fk_instance_user_user;

ALTER TABLE instance_user
    ADD CONSTRAINT fk_instance_user_user
        FOREIGN KEY (user_id)
            REFERENCES users(identifier)
            ON DELETE CASCADE;

ALTER TABLE instance_user
DROP CONSTRAINT IF EXISTS fk_instance_user_role;

ALTER TABLE instance_user
    ADD CONSTRAINT fk_instance_user_role
        FOREIGN KEY (instance_role_id)
            REFERENCES instance_role(identifier);



-- INSTANCE ENTITY TAG
CREATE TABLE IF NOT EXISTS instance_entity_tag
(
    instance_id UUID NOT NULL,
    entity_tag_id UUID NOT NULL,
    PRIMARY KEY (instance_id, entity_tag_id)
    );

ALTER TABLE instance_entity_tag
DROP CONSTRAINT IF EXISTS fk_iet_instance;

ALTER TABLE instance_entity_tag
    ADD CONSTRAINT fk_iet_instance
        FOREIGN KEY (instance_id)
            REFERENCES instance(identifier)
            ON DELETE CASCADE;

ALTER TABLE instance_entity_tag
DROP CONSTRAINT IF EXISTS fk_iet_entity_tag;

ALTER TABLE instance_entity_tag
    ADD CONSTRAINT fk_iet_entity_tag
        FOREIGN KEY (entity_tag_id)
            REFERENCES entity_tag(identifier)
            ON DELETE CASCADE;



-- INSTANCE LOCATION
CREATE TABLE IF NOT EXISTS instance_location
(
    instance_id UUID NOT NULL,
    location_id UUID NOT NULL,
    PRIMARY KEY (instance_id, location_id)
    );

ALTER TABLE instance_location
DROP CONSTRAINT IF EXISTS fk_il_instance;

ALTER TABLE instance_location
    ADD CONSTRAINT fk_il_instance
        FOREIGN KEY (instance_id)
            REFERENCES instance(identifier)
            ON DELETE CASCADE;

ALTER TABLE instance_location
DROP CONSTRAINT IF EXISTS fk_il_location;

ALTER TABLE instance_location
    ADD CONSTRAINT fk_il_location
        FOREIGN KEY (location_id)
            REFERENCES location(identifier)
            ON DELETE CASCADE;



-- PLAN UPDATE
ALTER TABLE IF EXISTS plan
    ADD COLUMN IF NOT EXISTS instance_identifier UUID;

ALTER TABLE plan
DROP CONSTRAINT IF EXISTS fk_plan_instance;

ALTER TABLE plan
    ADD CONSTRAINT fk_plan_instance
        FOREIGN KEY (instance_identifier)
            REFERENCES instance(identifier);



-- INDEXES
CREATE INDEX IF NOT EXISTS idx_iet_tag
    ON instance_entity_tag(entity_tag_id);

CREATE INDEX IF NOT EXISTS idx_il_location
    ON instance_location(location_id);

CREATE INDEX IF NOT EXISTS idx_iu_user
    ON instance_user(user_id);