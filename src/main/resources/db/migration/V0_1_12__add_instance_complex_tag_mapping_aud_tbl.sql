
CREATE TABLE IF NOT EXISTS complex_tag_ownership_aud (
                     id UUID NOT NULL,
                     rev INTEGER NOT NULL,
                     revtype SMALLINT,
                     complex_tag_id INTEGER,
                     user_sid UUID,
                     PRIMARY KEY (id, rev)
    );

ALTER TABLE complex_tag_ownership_aud
    ADD CONSTRAINT fk_cto_aud_rev
        FOREIGN KEY (rev) REFERENCES revinfo(rev);

-- =========================================
-- ComplexTagAccGrantsUser Audit Table
-- =========================================
CREATE TABLE IF NOT EXISTS complex_tag_acc_grants_user_aud (
           id UUID NOT NULL,
           rev INTEGER NOT NULL,
           revtype SMALLINT,
           complex_tag_id INTEGER,
           user_sid UUID,
           PRIMARY KEY (id, rev)
    );

ALTER TABLE complex_tag_acc_grants_user_aud
    ADD CONSTRAINT fk_ctagu_aud_rev
        FOREIGN KEY (rev) REFERENCES revinfo(rev);

-- =========================================
-- ComplexTagAccGrantsOrganization Audit Table
-- =========================================
CREATE TABLE IF NOT EXISTS complex_tag_acc_grants_org_aud (
          id UUID NOT NULL,
          rev INTEGER NOT NULL,
          revtype SMALLINT,
          complex_tag_id INTEGER,
          organization_id UUID,
          PRIMARY KEY (id, rev)
    );

ALTER TABLE complex_tag_acc_grants_org_aud
    ADD CONSTRAINT fk_ctago_aud_rev
        FOREIGN KEY (rev) REFERENCES revinfo(rev);

-- =========================================
-- Indexes (Recommended for performance)
-- =========================================
CREATE INDEX idx_cto_aud_rev ON complex_tag_ownership_aud(rev);
CREATE INDEX idx_ctagu_aud_rev ON complex_tag_acc_grants_user_aud(rev);
CREATE INDEX idx_ctago_aud_rev ON complex_tag_acc_grants_org_aud(rev);