CREATE TABLE IF NOT EXISTS report
(
    id               UUID  NOT NULL,
    location_id      UUID  NOT NULL,
    plan_id          UUID  NOT NULL,
    report_indicators JSONB NOT NULL,
    PRIMARY KEY (id)
);
