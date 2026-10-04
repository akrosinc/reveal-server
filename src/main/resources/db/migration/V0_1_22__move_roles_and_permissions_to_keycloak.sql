-- Migration: Move roles and permissions from database to Keycloak
-- Description:
--   1. Drops old foreign key constraints and indexes referencing database role tables
--   2. Role ID columns on instance_user and organization_role_mapping remain UUIDs for Keycloak group IDs
--   3. Drops obsolete role and permission tables and their audit tables
--
-- Manual Prerequisites in Keycloak:
--   - Create parent groups: '/ORGANIZATION_ROLES' (or '/organization-roles') and '/INSTANCE_ROLES' (or '/instance-roles')
--   - Under instance roles parent group, create child groups matching InstanceRoleEnum ('ADMIN', 'STANDARD')
--   - Under organization roles parent group, create child groups for each organization role
--   - Create client roles on the 'reveal-server' client corresponding to application permissions (e.g., PLAN_VIEW)
--   - Map the client roles to the appropriate role child groups in Keycloak

-- Step 1: Drop old foreign key constraints
ALTER TABLE IF EXISTS instance_user
    DROP CONSTRAINT IF EXISTS fk_instance_user_role;

ALTER TABLE IF EXISTS organization_role_mapping
    DROP CONSTRAINT IF EXISTS fk_org_role_map_role;

DROP INDEX IF EXISTS idx_org_role_mapping_role;

-- Optional backfill template (commented out):
-- If existing rows need to be mapped to Keycloak group IDs before dropping old role tables,
-- replace '<KEYCLOAK_GROUP_UUID_...>' with the actual Keycloak group UUIDs and execute:
--
-- UPDATE instance_user iu
-- SET instance_role_id = '<KEYCLOAK_GROUP_UUID_ADMIN>'::uuid
-- WHERE iu.instance_role_id IN (SELECT identifier FROM instance_role WHERE name = 'ADMIN');
--
-- UPDATE instance_user iu
-- SET instance_role_id = '<KEYCLOAK_GROUP_UUID_STANDARD>'::uuid
-- WHERE iu.instance_role_id IN (SELECT identifier FROM instance_role WHERE name = 'STANDARD');
--
-- UPDATE organization_role_mapping orm
-- SET organization_role_id = '<KEYCLOAK_GROUP_UUID_ORG_ROLE>'::uuid
-- WHERE orm.organization_role_id IN (SELECT identifier FROM organization_role WHERE name = '<ROLE_NAME>');

-- Step 2: Drop old role and permission tables (in dependency order)
DROP TABLE IF EXISTS organization_role_permission_aud CASCADE;
DROP TABLE IF EXISTS organization_role_permission CASCADE;
DROP TABLE IF EXISTS instance_role_permission_aud CASCADE;
DROP TABLE IF EXISTS instance_role_permission CASCADE;
DROP TABLE IF EXISTS permissions_aud CASCADE;
DROP TABLE IF EXISTS permission_aud CASCADE;
DROP TABLE IF EXISTS permissions CASCADE;
DROP TABLE IF EXISTS permission CASCADE;
DROP TABLE IF EXISTS organization_role_aud CASCADE;
DROP TABLE IF EXISTS organization_role CASCADE;
DROP TABLE IF EXISTS instance_role_aud CASCADE;
DROP TABLE IF EXISTS instance_role CASCADE;
