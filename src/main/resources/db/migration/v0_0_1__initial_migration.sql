/* plan definition */
BEGIN;

SET search_path TO :"schema",public;

CREATE EXTENSION IF NOT EXISTS "unaccent" WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;

CREATE TABLE IF NOT EXISTS plan (
    id VARCHAR(36) UNIQUE NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created__datetime TIMESTAMP WITH TIME ZONE NOT NULL,
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
    created__datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    PRIMARY KEY (id)
);