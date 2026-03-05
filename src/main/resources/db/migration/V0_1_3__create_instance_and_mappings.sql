CREATE TABLE instance
(
    identifier  UUID   NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    hierarchy_identifier UUID,

    entity_status     VARCHAR(36)              NOT NULL,
    created_by        VARCHAR(36)              NOT NULL,
    created_datetime  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by       VARCHAR(36)              NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier),

    CONSTRAINT fk_instance_hierarchy
        FOREIGN KEY (hierarchy_identifier)
        REFERENCES location_hierarchy(identifier)
);

CREATE TABLE instance_role (
   identifier UUID PRIMARY KEY,
   name VARCHAR(255)
);


CREATE TABLE permissions (
    identifier  UUID   NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (identifier)
);


CREATE TABLE instance_role_permission (
      instance_role_id UUID NOT NULL,
      permission_id UUID NOT NULL,
      PRIMARY KEY (instance_role_id, permission_id),
      CONSTRAINT fk_irp_role
          FOREIGN KEY (instance_role_id)
              REFERENCES instance_role(identifier)
              ON DELETE CASCADE,
      CONSTRAINT fk_irp_permission
          FOREIGN KEY (permission_id)
              REFERENCES permissions(identifier)
              ON DELETE CASCADE
);

CREATE TABLE instance_user
(

    instance_id UUID NOT NULL,
    user_id     UUID NOT NULL,
    instance_role_id UUID,
    PRIMARY KEY (instance_id, user_id),

    CONSTRAINT fk_instance_user_instance
        FOREIGN KEY (instance_id)
            REFERENCES instance(identifier)
            ON DELETE CASCADE,
    CONSTRAINT fk_instance_user_user
        FOREIGN KEY (user_id)
            REFERENCES users(identifier)
            ON DELETE CASCADE,
    CONSTRAINT fk_instance_user_role
        FOREIGN KEY (instance_role_id)
            REFERENCES instance_role(identifier)
);



CREATE TABLE instance_entity_tag
(
    instance_id   UUID NOT NULL,
    entity_tag_id UUID NOT NULL,

    PRIMARY KEY (instance_id, entity_tag_id),
    CONSTRAINT fk_iet_instance
        FOREIGN KEY (instance_id)
            REFERENCES instance(identifier)
            ON DELETE CASCADE,
    CONSTRAINT fk_iet_entity_tag
        FOREIGN KEY (entity_tag_id)
            REFERENCES entity_tag(identifier)
            ON DELETE CASCADE
);


CREATE TABLE instance_location
(
    instance_id UUID NOT NULL,
    location_id UUID NOT NULL,

    PRIMARY KEY (instance_id, location_id),

    CONSTRAINT fk_il_instance
        FOREIGN KEY (instance_id)
            REFERENCES instance(identifier)
            ON DELETE CASCADE,
    CONSTRAINT fk_il_location
        FOREIGN KEY (location_id)
            REFERENCES location(identifier)
            ON DELETE CASCADE
);






CREATE INDEX idx_iet_tag
    ON instance_entity_tag(entity_tag_id);

CREATE INDEX idx_il_location
    ON instance_location(location_id);

CREATE INDEX idx_iu_user
    ON instance_user(user_id);