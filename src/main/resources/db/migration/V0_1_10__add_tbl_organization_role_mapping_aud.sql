CREATE TABLE IF NOT EXISTS organization_role_mapping_aud
(
    organization_id      UUID    NOT NULL,
    organization_role_id UUID    NOT NULL,
    rev                  INTEGER NOT NULL,
    revtype              SMALLINT,
    PRIMARY KEY (organization_id, organization_role_id, rev)
    );

ALTER TABLE IF EXISTS organization_role_mapping_aud
DROP CONSTRAINT IF EXISTS fk_orm_aud_rev;

ALTER TABLE IF EXISTS organization_role_mapping_aud
    ADD CONSTRAINT fk_orm_aud_rev
    FOREIGN KEY (rev) REFERENCES revinfo (rev);