ALTER TABLE organization
    ADD COLUMN instance_id UUID;

ALTER TABLE organization
    ADD CONSTRAINT fk_organization_instance
        FOREIGN KEY (instance_id)
            REFERENCES instance(identifier)
            ON DELETE CASCADE;

CREATE TABLE organization_role (
   identifier UUID PRIMARY KEY,
   name VARCHAR(255) NOT NULL
);

CREATE TABLE organization_role_permission (
      organization_role_id UUID NOT NULL,
      permission_id UUID NOT NULL,
      PRIMARY KEY (organization_role_id, permission_id),
      CONSTRAINT fk_org_role_perm_role
          FOREIGN KEY (organization_role_id)
              REFERENCES organization_role(identifier)
              ON DELETE CASCADE,
      CONSTRAINT fk_org_role_perm_permission
          FOREIGN KEY (permission_id)
              REFERENCES permissions(identifier)
              ON DELETE CASCADE
);

CREATE TABLE organization_role_mapping (
   organization_id UUID NOT NULL,
   organization_role_id UUID NOT NULL,
   CONSTRAINT pk_organization_role_mapping PRIMARY KEY (organization_id, organization_role_id),
   CONSTRAINT fk_org_role_map_org
       FOREIGN KEY (organization_id)
           REFERENCES organization(identifier)
           ON DELETE CASCADE,
   CONSTRAINT fk_org_role_map_role
       FOREIGN KEY (organization_role_id)
           REFERENCES organization_role(identifier)
           ON DELETE CASCADE
);

CREATE INDEX idx_org_role_mapping_role
    ON organization_role_mapping(organization_role_id);

CREATE TABLE organization_location (
   organization_id UUID NOT NULL,
   location_id UUID NOT NULL,

   CONSTRAINT pk_organization_location
       PRIMARY KEY (organization_id, location_id),

   CONSTRAINT fk_org_location_organization
       FOREIGN KEY (organization_id)
           REFERENCES organization(identifier)
           ON DELETE CASCADE,


   CONSTRAINT fk_org_location_location
       FOREIGN KEY (location_id)
           REFERENCES location(identifier)
           ON DELETE CASCADE
);

CREATE INDEX idx_org_location_org
    ON organization_location(organization_id);