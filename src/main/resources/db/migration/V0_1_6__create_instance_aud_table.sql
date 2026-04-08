CREATE TABLE IF NOT EXISTS instance_aud
(
    identifier          UUID         NOT NULL,
    rev                 INTEGER      NOT NULL,
    revtype             SMALLINT,

    name                VARCHAR(255),
    hierarchy_identifier UUID,
    entity_status       VARCHAR(36),
    created_by          VARCHAR(36),
    created_datetime    TIMESTAMP WITH TIME ZONE,
                                      modified_by         VARCHAR(36),
    modified_datetime   TIMESTAMP WITH TIME ZONE,

                                      PRIMARY KEY (identifier, rev)
    );

ALTER TABLE IF EXISTS instance_aud
    DROP CONSTRAINT IF EXISTS fk_instance_aud_rev;

ALTER TABLE IF EXISTS instance_aud
    ADD CONSTRAINT fk_instance_aud_rev
        FOREIGN KEY (rev)
            REFERENCES revinfo (rev);

ALTER TABLE IF EXISTS plan_aud
    ADD COLUMN IF NOT EXISTS instance_identifier UUID;

ALTER TABLE IF EXISTS  location_hierarchy_aud
    ADD COLUMN IF NOT EXISTS is_base_hierarchy BOOLEAN;


-- INSTANCE_ENTITY_TAG AUDIT
CREATE TABLE IF NOT EXISTS instance_entity_tag_aud
(
    instance_id     UUID     NOT NULL,
    entity_tag_id   UUID     NOT NULL,
    rev             INTEGER  NOT NULL,
    revtype         SMALLINT,

    PRIMARY KEY (instance_id, entity_tag_id, rev)
    );

ALTER TABLE instance_entity_tag_aud
DROP CONSTRAINT IF EXISTS fk_iet_aud_rev;

ALTER TABLE instance_entity_tag_aud
    ADD CONSTRAINT fk_iet_aud_rev
        FOREIGN KEY (rev)
            REFERENCES revinfo (rev);


-- INSTANCE_USER AUDIT
CREATE TABLE IF NOT EXISTS instance_user_aud
(
    instance_id      UUID     NOT NULL,
    user_id          UUID     NOT NULL,
    rev              INTEGER  NOT NULL,
    revtype          SMALLINT,
    instance_role_id UUID,

    PRIMARY KEY (instance_id, user_id, rev)
    );

ALTER TABLE instance_user_aud
DROP CONSTRAINT IF EXISTS fk_iu_aud_rev;

ALTER TABLE instance_user_aud
    ADD CONSTRAINT fk_iu_aud_rev
        FOREIGN KEY (rev)
            REFERENCES revinfo (rev);


-- INSTANCE_LOCATION AUDIT
CREATE TABLE IF NOT EXISTS instance_location_aud
(
    instance_id  UUID     NOT NULL,
    location_id  UUID     NOT NULL,
    rev          INTEGER  NOT NULL,
    revtype      SMALLINT,

    PRIMARY KEY (instance_id, location_id, rev)
    );

ALTER TABLE instance_location_aud
DROP CONSTRAINT IF EXISTS fk_il_aud_rev;

ALTER TABLE instance_location_aud
    ADD CONSTRAINT fk_il_aud_rev
        FOREIGN KEY (rev)
            REFERENCES revinfo (rev);