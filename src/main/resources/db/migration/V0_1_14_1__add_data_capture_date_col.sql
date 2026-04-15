-- ADD data_capture_date TO import_aggregation_numeric
ALTER TABLE  IF EXISTS import_aggregation_numeric
    ADD COLUMN IF NOT EXISTS data_capture_date DATE;

-- ADD data_capture_date TO import_aggregation_string
ALTER TABLE IF EXISTS import_aggregation_string
    ADD COLUMN IF NOT EXISTS data_capture_date DATE;

-- INDEX on data_capture_date for faster queries
CREATE INDEX IF NOT EXISTS idx_import_aggregation_numeric_data_capture_date
    ON import_aggregation_numeric (data_capture_date);

CREATE INDEX IF NOT EXISTS idx_import_aggregation_string_data_capture_date
    ON import_aggregation_string (data_capture_date);

-- COMPOSITE INDEX for common query pattern
CREATE INDEX IF NOT EXISTS idx_import_aggregation_numeric_composite
    ON import_aggregation_numeric (hierarchy_identifier, field_code, data_capture_date);

CREATE INDEX IF NOT EXISTS idx_import_aggregation_string_composite
    ON import_aggregation_string (hierarchy_identifier, field_code, data_capture_date);