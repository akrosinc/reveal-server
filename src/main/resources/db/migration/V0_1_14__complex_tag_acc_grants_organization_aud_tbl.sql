DROP TABLE IF EXISTS complex_tag_acc_grants_organization_aud CASCADE;


CREATE TABLE IF NOT EXISTS complex_tag_acc_grants_organization_aud (
   id UUID NOT NULL,
   rev INTEGER NOT NULL,
   revtype SMALLINT,

   complex_tag_id INTEGER,
   organization_id UUID,

   PRIMARY KEY (id, rev)
    );

ALTER TABLE complex_tag_aud
DROP CONSTRAINT IF EXISTS fk_complex_tag_aud_rev;

ALTER TABLE complex_tag_acc_grants_organization_aud
    ADD CONSTRAINT fk_complex_tag_aud_rev
        FOREIGN KEY (rev) REFERENCES revinfo(rev);

DROP INDEX IF EXISTS idx_complex_tag_org_aud_rev;

CREATE INDEX idx_complex_tag_org_aud_rev ON complex_tag_acc_grants_organization_aud(rev);