alter table if exists amdr.amdr_header_names
    add column if not exists col_type varchar;

alter table if exists amdr.amdr_header_names
    add column if not exists col_parent varchar;

-- KELCH13 (green)
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

            -- KELCH13 (green)
            UPDATE amdr.amdr_header_names
            SET col_type = 'DRUG'
            WHERE key = 'kelch13';

            -- PFCRT (red)
            UPDATE amdr.amdr_header_names
            SET col_type = 'DRUG'
            WHERE key = 'pfcrt';

            UPDATE amdr.amdr_header_names
            SET col_type = 'HAPLOTYPE'
            WHERE key = 'pfcrt_72';

            UPDATE amdr.amdr_header_names
            SET col_type = 'HAPLOTYPE'
            WHERE key = 'pfcrt_74';

            UPDATE amdr.amdr_header_names
            SET col_type = 'HAPLOTYPE'
            WHERE key = 'pfcrt_75';

            UPDATE amdr.amdr_header_names
            SET col_type = 'HAPLOTYPE'
            WHERE key = 'pfcrt_76';

            -- PFMDR1 (blue)
            UPDATE amdr.amdr_header_names
            SET col_type = 'DRUG'
            WHERE key = 'pfmdr1';

            UPDATE amdr.amdr_header_names
            SET col_type = 'HAPLOTYPE'
            WHERE key = 'pfmdr1_86';

            UPDATE amdr.amdr_header_names
            SET col_type = 'HAPLOTYPE'
            WHERE key = 'pfmdr1_184';

            UPDATE amdr.amdr_header_names
            SET col_type = 'HAPLOTYPE'
            WHERE key = 'pfmdr1_1246';

            -- CRT + MDR1 (purple)
            UPDATE amdr.amdr_header_names
            SET col_type = 'DRUG'
            WHERE key = 'crt_mdr1';

            -- PFDHFR (orange)
            UPDATE amdr.amdr_header_names
            SET col_type = 'DRUG'
            WHERE key = 'pfdhfr';

            UPDATE amdr.amdr_header_names
            SET col_type = 'HAPLOTYPE'
            WHERE key = 'pfdhfr_51';

            UPDATE amdr.amdr_header_names
            SET col_type = 'HAPLOTYPE'
            WHERE key = 'pfdhfr_59';

            UPDATE amdr.amdr_header_names
            SET col_type = 'HAPLOTYPE'
            WHERE key = 'pfdhfr_108';

            UPDATE amdr.amdr_header_names
            SET col_type = 'HAPLOTYPE'
            WHERE key = 'pfdhfr_164';

            -- DHFR (amber)
            UPDATE amdr.amdr_header_names
            SET col_type = 'DRUG'
            WHERE key = 'dhfr';

            -- PFDHPS (teal)
            UPDATE amdr.amdr_header_names
            SET col_type = 'DRUG'
            WHERE key = 'pfdhps';

            UPDATE amdr.amdr_header_names
            SET col_type = 'HAPLOTYPE'
            WHERE key = 'pfdhps_436';

            UPDATE amdr.amdr_header_names
            SET col_type = 'HAPLOTYPE'
            WHERE key = 'pfdhps_437';

            UPDATE amdr.amdr_header_names
            SET col_type = 'HAPLOTYPE'
            WHERE key = 'pfdhps_540';

            UPDATE amdr.amdr_header_names
            SET col_type = 'HAPLOTYPE'
            WHERE key = 'pfdhps_581';

            UPDATE amdr.amdr_header_names
            SET col_type = 'HAPLOTYPE'
            WHERE key = 'pfdhps_613';

            -- DHFR + DHPS (brown)
            UPDATE amdr.amdr_header_names
            SET col_type = 'DRUG'
            WHERE key = 'dhfr_dhps';

        END IF;
    END $$;
