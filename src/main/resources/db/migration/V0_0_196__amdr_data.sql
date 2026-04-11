INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('kelch13'::text, 'Artemisinin [Kelch13]'::text)
ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfcrt'::text, 'Chloroquine Resistance [PfCRT]'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfcrt_72'::text, 'CRT C72S'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfcrt_74'::text, 'CRT M74I'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfcrt_75'::text, 'CRT N75E'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfcrt_76'::text, 'CRT K76T'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfmdr1'::text, 'Multidrug PfMDR1'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfmdr1_86'::text, 'MDR1 N86Y'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfmdr1_184'::text, 'MDR1 Y184F'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfmdr1_1246'::text, 'MDR1 D1246Y'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('crt_mdr1'::text, 'Partner drug Resistance'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfdhfr'::text, 'Pyrimethamine Resistance [PfDHFR]'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfdhfr_51'::text, 'DHFR N51I'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfdhfr_59'::text, 'DHFR C59R'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfdhfr_108'::text, 'DHFR S108N'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfdhfr_164'::text, 'DHFR I164L'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('dhfr'::text, 'SP Resistance [DHFR]'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfdhps'::text, 'Sulfadoxine Resistance [PfDHPS]'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfdhps_436'::text, 'DHPS S436A'::text)
ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfdhps_437'::text, 'DHPS A437G'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfdhps_540'::text, 'DHPS K540E'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfdhps_581'::text, 'DHPS A581G'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('pfdhps_613'::text, 'DHPS A613S'::text)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_header_names (key, name)
VALUES ('dhfr_dhps'::text, 'SP-IPTp Resistance [DHFR+DHPS]'::text)
    ON CONFLICT DO NOTHING;


INSERT INTO amdr.amdr_mappings (id, amdr_key, amdr_sub_keys)
VALUES (3::integer, 'kelch13'::varchar, '{"kelch13": ["kelch13"]}'::jsonb)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_mappings (id, amdr_key, amdr_sub_keys)
VALUES (8::integer, 'dhfr'::varchar,
        '{"dhfr": ["pfdhfr_51", "pfdhfr_59", "pfdhfr_108", "pfdhfr_164"]}'::jsonb)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_mappings (id, amdr_key, amdr_sub_keys)
VALUES (2::integer, 'pfdhps'::varchar,
        '{"pfdhps": ["pfdhps_436", "pfdhps_437", "pfdhps_540", "pfdhps_581", "pfdhps_613"]}'::jsonb)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_mappings (id, amdr_key, amdr_sub_keys)
VALUES (5::integer, 'pfmdr1'::varchar,
        '{"pfmdr1": ["pfmdr1_86", "pfmdr1_184", "pfmdr1_1246"]}'::jsonb)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_mappings (id, amdr_key, amdr_sub_keys)
VALUES (4::integer, 'pfcrt'::varchar,
        '{"pfcrt": ["pfcrt_72", "pfcrt_74", "pfcrt_75", "pfcrt_76"]}'::jsonb)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_mappings (id, amdr_key, amdr_sub_keys)
VALUES (1::integer, 'dhfr_dhps'::varchar,
        '{"dhfr_dhps": ["pfdhfr_51", "pfdhfr_59", "pfdhfr_108", "pfdhfr_164", "pfdhps_437", "pfdhps_540", "pfdhps_581", "pfdhps_613"]}'::jsonb)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_mappings (id, amdr_key, amdr_sub_keys)
VALUES (7::integer, 'pfdhfr'::varchar,
        '{"dhfr": ["pfdhfr_51", "pfdhfr_59", "pfdhfr_108", "pfdhfr_164"], "pfdhfr": []}'::jsonb)
    ON CONFLICT DO NOTHING;

INSERT INTO amdr.amdr_mappings (id, amdr_key, amdr_sub_keys)
VALUES (6::integer, 'crt_mdr1'::varchar,
        '{"pfcrt": ["pfcrt_72", "pfcrt_74", "pfcrt_75"], "pfmdr1": ["pfmdr1_86", "pfmdr1_184", "pfmdr1_1246"], "crt_mdr1": []}'::jsonb)
    ON CONFLICT DO NOTHING;

