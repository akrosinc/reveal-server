alter table if exists amdr.amdr_header_names
    add column if not exists contributing_col varchar[];

DO $$
BEGIN
        IF EXISTS (
                   SELECT 1
                   FROM information_schema.tables
                   WHERE table_schema = 'amdr'
                     AND table_name = 'amdr_header_names'
               )
            AND EXISTS (
                   SELECT 1
                   FROM information_schema.columns
                   WHERE table_schema = 'amdr'
                     AND table_name = 'amdr_header_names'
                     AND column_name = 'col_type'
               )
            AND EXISTS (
                   SELECT 1
                   FROM information_schema.columns
                   WHERE table_schema = 'amdr'
                     AND table_name = 'amdr_header_names'
                     AND column_name = 'col_parent'
               ) THEN

UPDATE amdr.amdr_header_names
SET contributing_col = ARRAY['kelch13']
WHERE key = 'kelch13' and col_type = 'DRUG';

UPDATE amdr.amdr_header_names
SET contributing_col = ARRAY['pfcrt_76']
WHERE key = 'pfcrt' and col_type = 'DRUG';

UPDATE amdr.amdr_header_names
SET contributing_col = ARRAY['pfdhfr_108']
WHERE key = 'pfdhfr' and col_type = 'DRUG';

UPDATE amdr.amdr_header_names
SET contributing_col = ARRAY['pfdhps_437']
WHERE key = 'pfdhps' and col_type = 'DRUG';

UPDATE amdr.amdr_header_names
SET contributing_col = ARRAY['pfmdr1_86']
WHERE key = 'pfmdr1' and col_type = 'DRUG';

UPDATE amdr.amdr_header_names
SET contributing_col = ARRAY['pfdhfr_51','pfdhfr_59','pfdhfr_108']
WHERE key = 'dhfr'  and col_type = 'DRUG';

UPDATE amdr.amdr_header_names
SET contributing_col = ARRAY['pfdhfr_51','pfdhfr_59','pfdhfr_108', 'pfdhps_437', 'pfdhps_540', 'pfdhfr_164','pfdhps_581', 'pfdhps_613']
WHERE key = 'dhfr_dhps'  and col_type = 'DRUG';

END IF;
END $$;
