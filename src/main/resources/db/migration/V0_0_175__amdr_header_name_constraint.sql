ALTER TABLE IF EXISTS amdr.amdr_header_names
DROP CONSTRAINT IF EXISTS amdr_header_names_pk;

DO $$
BEGIN
        IF NOT EXISTS (
                SELECT 1
                FROM pg_constraint
                WHERE conname = 'amdr_header_names_pk'
            ) THEN
ALTER TABLE amdr.amdr_header_names
    ADD CONSTRAINT amdr_header_names_pk
        PRIMARY KEY (key, col_type);
END IF;
END
$$;

INSERT INTO amdr.amdr_header_names (key, name, col_type, color)
VALUES ('kelch13'::text, 'Kelch'::text,'HAPLOTYPE'::text,'{"h":120,"s":100,"l":50}'::jsonb)
    ON CONFLICT DO NOTHING;
