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

CREATE SEQUENCE plan_seq INCREMENT 50;
CREATE TABLE IF NOT EXISTS plan (
    id BIGINT UNIQUE NOT NULL,
    identifier VARCHAR(36),
    name VARCHAR(36),
    title VARCHAR(36),
    status VARCHAR(36),
    date TIMESTAMP,
    effective_period_start DATE,
    effective_period_end DATE,
    intervention_type VARCHAR(36),
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS plan_aud (
    id BIGINT NOT NULL,
    REV INT NOT NULL,
    REVTYPE INTEGER NULL,
    identifier VARCHAR(36),
    name VARCHAR(36),
    title VARCHAR(36),
    status VARCHAR(36),
    date TIMESTAMP,
    effective_period_start DATE,
    effective_period_end DATE,
    intervention_type VARCHAR(36),
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS raster_store (
    id BIGINT NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(36) NOT NULL,
    modified_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    rid serial primary key,
    rast raster,
    file_name VARCHAR(36)

);

CREATE INDEX IF NOT EXISTS raster_store_idx ON raster_store(id);
CREATE INDEX raster_store_rast_st_convexhull_idx ON raster_store USING gist( public.ST_ConvexHull(rast) );
