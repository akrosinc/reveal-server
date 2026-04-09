CREATE TABLE IF NOT EXISTS instance_complex_tag
(
    instance_id    UUID    NOT NULL,
    complex_tag_id INTEGER NOT NULL,
    PRIMARY KEY (instance_id, complex_tag_id)
    );

ALTER TABLE IF EXISTS instance_complex_tag
DROP CONSTRAINT IF EXISTS fk_ict_instance;

ALTER TABLE IF EXISTS instance_complex_tag
    ADD CONSTRAINT fk_ict_instance
    FOREIGN KEY (instance_id)
    REFERENCES instance (identifier)
    ON DELETE CASCADE;

ALTER TABLE IF EXISTS instance_complex_tag
DROP CONSTRAINT IF EXISTS fk_ict_complex_tag;

ALTER TABLE IF EXISTS instance_complex_tag
    ADD CONSTRAINT fk_ict_complex_tag
    FOREIGN KEY (complex_tag_id)
    REFERENCES complex_tag (id)
    ON DELETE CASCADE;

-- INDEX
CREATE INDEX IF NOT EXISTS idx_ict_complex_tag
    ON instance_complex_tag (complex_tag_id);


CREATE TABLE IF NOT EXISTS instance_complex_tag_aud
(
    instance_id    UUID    NOT NULL,
    complex_tag_id INTEGER NOT NULL,
    rev            INTEGER NOT NULL,
    revtype        SMALLINT,
    PRIMARY KEY (instance_id, complex_tag_id, rev)
    );

ALTER TABLE IF EXISTS instance_complex_tag_aud
DROP CONSTRAINT IF EXISTS fk_ict_aud_rev;

ALTER TABLE IF EXISTS instance_complex_tag_aud
    ADD CONSTRAINT fk_ict_aud_rev
    FOREIGN KEY (rev)
    REFERENCES revinfo (rev);