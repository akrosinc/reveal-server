alter table if exists amdr.amdr_header_names
    add column if not exists color jsonb;


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
                     AND column_name = 'color'
               ) THEN

            -- KELCH13 (green)
            UPDATE amdr.amdr_header_names
            SET color = '{"h":120,"s":100,"l":50}'::jsonb
            WHERE key = 'kelch13';

            -- PFCRT (red)
            UPDATE amdr.amdr_header_names
            SET color = '{"h":0,"s":100,"l":50}'::jsonb
            WHERE key = 'pfcrt';

            UPDATE amdr.amdr_header_names
            SET color = '{"h":0,"s":50,"l":15}'::jsonb
            WHERE key = 'pfcrt_72';

            UPDATE amdr.amdr_header_names
            SET color = '{"h":0,"s":50,"l":30}'::jsonb
            WHERE key = 'pfcrt_74';

            UPDATE amdr.amdr_header_names
            SET color = '{"h":0,"s":50,"l":70}'::jsonb
            WHERE key = 'pfcrt_75';

            UPDATE amdr.amdr_header_names
            SET color = '{"h":0,"s":50,"l":85}'::jsonb
            WHERE key = 'pfcrt_76';

            -- PFMDR1 (blue)
            UPDATE amdr.amdr_header_names
            SET color = '{"h":210,"s":100,"l":50}'::jsonb
            WHERE key = 'pfmdr1';

            UPDATE amdr.amdr_header_names
            SET color = '{"h":203,"s":38,"l":25}'::jsonb
            WHERE key = 'pfmdr1_86';

            UPDATE amdr.amdr_header_names
            SET color = '{"h":203,"s":38,"l":50}'::jsonb
            WHERE key = 'pfmdr1_184';

            UPDATE amdr.amdr_header_names
            SET color = '{"h":203,"s":38,"l":75}'::jsonb
            WHERE key = 'pfmdr1_1246';

            -- CRT + MDR1 (purple)
            UPDATE amdr.amdr_header_names
            SET color = '{"h":270,"s":100,"l":50}'::jsonb
            WHERE key = 'crt_mdr1';

            -- PFDHFR (orange)
            UPDATE amdr.amdr_header_names
            SET color = '{"h":30,"s":100,"l":50}'::jsonb
            WHERE key = 'pfdhfr';

            UPDATE amdr.amdr_header_names
            SET color = '{"h":30,"s":50,"l":15}'::jsonb
            WHERE key = 'pfdhfr_51';

            UPDATE amdr.amdr_header_names
            SET color = '{"h":30,"s":50,"l":35}'::jsonb
            WHERE key = 'pfdhfr_59';

            UPDATE amdr.amdr_header_names
            SET color = '{"h":30,"s":50,"l":55}'::jsonb
            WHERE key = 'pfdhfr_108';

            UPDATE amdr.amdr_header_names
            SET color = '{"h":30,"s":50,"l":80}'::jsonb
            WHERE key = 'pfdhfr_164';

            -- DHFR (amber)
            UPDATE amdr.amdr_header_names
            SET color = '{"h":40,"s":100,"l":50}'::jsonb
            WHERE key = 'dhfr';

            -- PFDHPS (teal)
            UPDATE amdr.amdr_header_names
            SET color = '{"h":170,"s":100,"l":50}'::jsonb
            WHERE key = 'pfdhps';

            UPDATE amdr.amdr_header_names
            SET color = '{"h":180,"s":38,"l":12}'::jsonb
            WHERE key = 'pfdhps_436';

            UPDATE amdr.amdr_header_names
            SET color = '{"h":170,"s":38,"l":28}'::jsonb
            WHERE key = 'pfdhps_437';

            UPDATE amdr.amdr_header_names
            SET color = '{"h":170,"s":38,"l":45}'::jsonb
            WHERE key = 'pfdhps_540';

            UPDATE amdr.amdr_header_names
            SET color = '{"h":170,"s":38,"l":65}'::jsonb
            WHERE key = 'pfdhps_581';

            UPDATE amdr.amdr_header_names
            SET color = '{"h":170,"s":38,"l":85}'::jsonb
            WHERE key = 'pfdhps_613';

            -- DHFR + DHPS (brown)
            UPDATE amdr.amdr_header_names
            SET color = '{"h":20,"s":100,"l":50}'::jsonb
            WHERE key = 'dhfr_dhps';

        END IF;
    END $$;
