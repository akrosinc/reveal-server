CREATE TABLE IF NOT EXISTS organization_location_aud
(
    organization_id UUID    NOT NULL,
    location_id     UUID    NOT NULL,
    rev             INTEGER NOT NULL,
    revtype         SMALLINT,
    PRIMARY KEY (organization_id, location_id, rev)
    );

ALTER TABLE IF EXISTS organization_location_aud
DROP CONSTRAINT IF EXISTS fk_ol_aud_rev;

ALTER TABLE IF EXISTS organization_location_aud
    ADD CONSTRAINT fk_ol_aud_rev
    FOREIGN KEY (rev) REFERENCES revinfo (rev);