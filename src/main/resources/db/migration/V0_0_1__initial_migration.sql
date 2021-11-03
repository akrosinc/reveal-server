/* plan definition */
BEGIN;

SET search_path TO :"schema",public;

CREATE EXTENSION IF NOT EXISTS "unaccent" WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS postgis_raster WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;

CREATE TABLE IF NOT EXISTS task (
    id VARCHAR(36) UNIQUE NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS task_idx ON task(id);
CREATE INDEX IF NOT EXISTS task_payload_idx ON task((payload ->> 'identifier'));
CREATE INDEX IF NOT EXISTS task_payload_plan_idx ON task((payload ->> 'planIdentifier'));
CREATE INDEX IF NOT EXISTS task_payload_group_idx ON task((payload ->> 'groupIdentifier'));
CREATE INDEX IF NOT EXISTS task_payload_focus ON task((payload ->> 'focus'));
CREATE INDEX IF NOT EXISTS task_payload_status ON task((payload ->> 'status'));
CREATE INDEX IF NOT EXISTS task_payload_code ON task((payload ->> 'code'));

CREATE TABLE IF NOT EXISTS task_aud (
    id VARCHAR(36) NOT NULL,
    REV INT NOT NULL,
    REVTYPE TINYINT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS plan (
    id VARCHAR(36) UNIQUE NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS plan_aud (
    id VARCHAR(36) NOT NULL,
    REV INT NOT NULL,
    REVTYPE TINYINT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS raster_store (
    id VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    rid serial primary key,
    rast raster,
    file_name VARCHAR(36)

);

CREATE INDEX IF NOT EXISTS raster_store_idx ON raster_store(id);
CREATE INDEX raster_store_rast_st_convexhull_idx ON raster_store USING gist( ST_ConvexHull(rast) );
