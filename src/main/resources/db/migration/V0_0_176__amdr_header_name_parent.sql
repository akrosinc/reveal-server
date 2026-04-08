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
SET col_parent = 'kelch13'
WHERE key = 'kelch13' and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_parent = 'pfcrt'
WHERE key = 'pfcrt_72' and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_parent = 'pfcrt'
WHERE key = 'pfcrt_74' and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_parent = 'pfcrt'
WHERE key = 'pfcrt_75' and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_parent = 'pfcrt'
WHERE key = 'pfcrt_76'and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_parent = 'pfmdr1'
WHERE key = 'pfmdr1_86' and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_parent = 'pfmdr1'
WHERE key = 'pfmdr1_184' and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_parent = 'pfmdr1'
WHERE key = 'pfmdr1_1246' and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_parent = 'pfdhfr'
WHERE key = 'pfdhfr_51'  and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_parent = 'pfdhfr'
WHERE key = 'pfdhfr_59'  and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_parent = 'pfdhfr'
WHERE key = 'pfdhfr_108'  and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_parent = 'pfdhfr'
WHERE key = 'pfdhfr_164'  and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_parent = 'pfdhps'
WHERE key = 'pfdhps_436'  and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_parent = 'pfdhps'
WHERE key = 'pfdhps_437' and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_parent = 'pfdhps'
WHERE key = 'pfdhps_540'  and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_parent = 'pfdhps'
WHERE key = 'pfdhps_581'  and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_parent = 'pfdhps'
WHERE key = 'pfdhps_613'  and col_type = 'HAPLOTYPE';


END IF;
END $$;
