CREATE EXTENSION IF NOT EXISTS "unaccent" WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS postgis_raster WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;

CREATE TABLE revinfo (
    rev INTEGER PRIMARY KEY DEFAULT 1,
    revtstmp BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task (
    identifier VARCHAR(36) NOT NULL,
    entity_status VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    plan_identifier VARCHAR(36) NOT NULL,
    focus VARCHAR(36) NOT NULL,
    code VARCHAR(36) NOT NULL,
    status VARCHAR(36) NOT NULL,
    priority VARCHAR(36) NOT NULL,
    authored_on TIMESTAMP WITH TIME ZONE NOT NULL,
    description VARCHAR(255) NOT NULL,
    last_modified TIMESTAMP WITH TIME ZONE NOT NULL,
    business_status VARCHAR(36) NOT NULL,
    execution_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    execution_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    group_identifier VARCHAR(36) NOT NULL,
    instantiates_uri VARCHAR(36) NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE INDEX IF NOT EXISTS task_idx ON task(identifier);
CREATE INDEX IF NOT EXISTS task_plan_identifier_idx ON task(plan_identifier);
CREATE INDEX IF NOT EXISTS task_status_idx ON task(status);
CREATE INDEX IF NOT EXISTS task_business_status_idx ON task(business_status);

CREATE TABLE IF NOT EXISTS task_aud (
    identifier VARCHAR(36) NOT NULL,
    REV INT NOT NULL,
    REVTYPE INTEGER NULL,
    entity_status VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    plan_identifier VARCHAR(36) NOT NULL,
    focus VARCHAR(36) NOT NULL,
    code VARCHAR(36) NOT NULL,
    status VARCHAR(36) NOT NULL,
    priority VARCHAR(36) NOT NULL,
    authored_on TIMESTAMP WITH TIME ZONE NOT NULL,
    description VARCHAR(255) NOT NULL,
    last_modified TIMESTAMP WITH TIME ZONE NOT NULL,
    business_status VARCHAR(36) NOT NULL,
    execution_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    execution_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    group_identifier VARCHAR(36) NOT NULL,
    instantiates_uri VARCHAR(36) NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS plan (
    identifier UUID UNIQUE NOT NULL,
    name VARCHAR(36),
    title VARCHAR(36),
    status VARCHAR(36),
    date TIMESTAMP,
    effective_period_start DATE,
    effective_period_end DATE,
    intervention_type VARCHAR(36),
    entity_status VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS plan_aud (
    identifier UUID NOT NULL,
    REV INT NOT NULL,
    REVTYPE INTEGER NULL,
    name VARCHAR(36),
    title VARCHAR(36),
    status VARCHAR(36),
    date TIMESTAMP,
    effective_period_start DATE,
    effective_period_end DATE,
    intervention_type VARCHAR(36),
    entity_status VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier,REV)
);

CREATE TABLE IF NOT EXISTS geographic_level(
  identifier UUID UNIQUE NOT NULL,
  name VARCHAR(255) UNIQUE,
  title VARCHAR(255),
  entity_status VARCHAR(36) NOT NULL,
  created_by VARCHAR(36) NOT NULL,
  created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
  modified_by VARCHAR(36) NOT NULL,
  modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
  PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS geographic_level_aud(
    identifier UUID NOT NULL,
    REV INT NOT NULL,
    REVTYPE INTEGER NULL,
    name VARCHAR(255),
    title VARCHAR(255),
    entity_status VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier,REV)
);

CREATE TABLE IF NOT EXISTS location_hierarchy(
     identifier UUID UNIQUE NOT NULL,
     node_order VARCHAR(255),
     entity_status VARCHAR(36) NOT NULL,
     created_by VARCHAR(36) NOT NULL,
     created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
     modified_by VARCHAR(36) NOT NULL,
     modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
     PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS location_hierarchy_aud(
    identifier UUID NOT NULL,
    REV INT NOT NULL,
    REVTYPE INTEGER NULL,
    node_order VARCHAR(255),
    entity_status VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier,REV)
);

CREATE TABLE IF NOT EXISTS location(
    identifier UUID UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    geometry jsonb NOT NULL,
    type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    external_id UUID,
    geographic_level_id UUID,
    entity_status VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS location_aud(
    identifier UUID NOT NULL,
    REV INT NOT NULL,
    REVTYPE INTEGER NULL,
    name VARCHAR(255) NOT NULL,
    geometry jsonb NOT NULL,
    type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    external_id UUID,
    geographic_level_id UUID,
    entity_status VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier,REV)
);

CREATE TABLE IF NOT EXISTS location_relationship(
    identifier UUID UNIQUE NOT NULL,
    location_hierarchy_identifier UUID NOT NULL,
    location_identifier UUID NOT NULL,
    parent_identifier UUID NOT NULL,
    ancestry VARCHAR(255) NOT NULL,
    entity_status VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS location_relationship_aud(
    identifier UUID NOT NULL,
    REV INT NOT NULL,
    REVTYPE INTEGER NULL,
    location_hierarchy_identifier UUID NOT NULL,
    location_identifier UUID NOT NULL,
    parent_identifier UUID NOT NULL,
    ancestry VARCHAR(255) NOT NULL,
    entity_status VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier,REV)
);

CREATE TABLE IF NOT EXISTS raster_store (
    id BIGINT NOT NULL,
    entity_status VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    rid serial primary key,
    rast raster,
    file_name VARCHAR(36)
);

CREATE TABLE IF NOT EXISTS organization(
    identifier UUID NOT NULL,
    active BOOLEAN,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(36) NOT NULL,
    organization_parent_id UUID,
    entity_status VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS organization_aud(
    identifier UUID NOT NULL,
    REV INT NOT NULL,
    REVTYPE INTEGER NULL,
    active BOOLEAN,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(36) NOT NULL,
    organization_parent_id UUID,
    entity_status VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier,REV)
);

CREATE TABLE IF NOT EXISTS "user"(
    identifier UUID NOT NULL,
    sid UUID,
    username VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    remote_state VARCHAR(64) NOT NULL,
    entity_status VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS user_aud(
    identifier UUID NOT NULL,
    REV INT NOT NULL,
    REVTYPE INTEGER NULL,
    sid UUID,
    username VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    remote_state VARCHAR(64) NOT NULL,
    entity_status VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (identifier, REV)
);

CREATE TABLE IF NOT EXISTS "user_groups"(
    user_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    CONSTRAINT user_groups_pk PRIMARY KEY (user_id, organization_id),
    FOREIGN KEY (user_id) REFERENCES "user"(identifier),
    FOREIGN KEY (organization_id) REFERENCES organization(identifier)
);

CREATE TABLE IF NOT EXISTS user_security_groups(
    identifier UUID NOT NULL,
    security_group VARCHAR(255),
    FOREIGN KEY (identifier) REFERENCES "user"(identifier)
);

CREATE SEQUENCE IF NOT EXISTS hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    MAXVALUE 9223372036854775807;

CREATE INDEX IF NOT EXISTS raster_store_idx ON raster_store(id);
CREATE INDEX raster_store_rast_st_convexhull_idx ON raster_store USING gist( public.ST_ConvexHull(rast) );
