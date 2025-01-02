
ALTER TABLE  IF EXISTS data_extract_query
    ADD COLUMN  IF NOT EXISTS query_label  varchar;

