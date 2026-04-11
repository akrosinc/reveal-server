-- =========================================
-- ComplexTag Audit Table
-- =========================================
CREATE TABLE IF NOT EXISTS complex_tag_aud (
   id INTEGER NOT NULL,
   rev INTEGER NOT NULL,
   revtype SMALLINT,

   hierarchy_id VARCHAR(255),
    hierarchy_type VARCHAR(255),
    tag_name VARCHAR(255),

    tags JSONB,
    formula TEXT,
    is_public BOOLEAN,

    PRIMARY KEY (id, rev)
    );

-- =========================================
-- Foreign Key to Revision Table
-- =========================================
ALTER TABLE complex_tag_aud
    ADD CONSTRAINT fk_complex_tag_aud_rev
        FOREIGN KEY (rev) REFERENCES revinfo(rev);

-- =========================================
-- Indexes (Recommended)
-- =========================================
CREATE INDEX idx_complex_tag_aud_rev ON complex_tag_aud(rev);