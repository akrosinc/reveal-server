alter table if exists amdr.amdr_header_names
    add column if not exists col_order int;

UPDATE amdr.amdr_header_names
SET col_order = 1
WHERE key = 'kelch13' and col_type = 'DRUG';

UPDATE amdr.amdr_header_names
SET col_order = 2
WHERE key = 'pfcrt' and col_type = 'DRUG';

UPDATE amdr.amdr_header_names
SET col_order = 3
WHERE key = 'pfmdr1' and col_type = 'DRUG';

UPDATE amdr.amdr_header_names
SET col_order = 4
WHERE key = 'crt_mdr1' and col_type = 'DRUG';

UPDATE amdr.amdr_header_names
SET col_order = 5
WHERE key = 'pfdhfr' and col_type = 'DRUG';

UPDATE amdr.amdr_header_names
SET col_order = 6
WHERE key = 'dhfr' and col_type = 'DRUG';

UPDATE amdr.amdr_header_names
SET col_order = 7
WHERE key = 'pfdhps' and col_type = 'DRUG';

UPDATE amdr.amdr_header_names
SET col_order = 8
WHERE key = 'dhfr_dhps' and col_type = 'DRUG';

UPDATE amdr.amdr_header_names
SET col_order = 9
WHERE key = 'kelch13' and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_order = 10
WHERE key = 'pfcrt_72' and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_order = 11
WHERE key = 'pfcrt_74' and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_order = 12
WHERE key = 'pfcrt_75' and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_order = 13
WHERE key = 'pfcrt_76'and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_order = 14
WHERE key = 'pfmdr1_86' and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_order = 15
WHERE key = 'pfmdr1_184' and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_order = 16
WHERE key = 'pfmdr1_1246' and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_order = 17
WHERE key = 'pfdhfr_51'  and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_order = 18
WHERE key = 'pfdhfr_59'  and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_order = 19
WHERE key = 'pfdhfr_108'  and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_order = 20
WHERE key = 'pfdhfr_164'  and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_order = 21
WHERE key = 'pfdhps_436'  and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_order = 22
WHERE key = 'pfdhps_437' and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_order = 23
WHERE key = 'pfdhps_540'  and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_order = 24
WHERE key = 'pfdhps_581'  and col_type = 'HAPLOTYPE';

UPDATE amdr.amdr_header_names
SET col_order = 25
WHERE key = 'pfdhps_613'  and col_type = 'HAPLOTYPE';
