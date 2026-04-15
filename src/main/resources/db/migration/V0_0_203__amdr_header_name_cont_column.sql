UPDATE amdr.amdr_header_names
SET contributing_col = ARRAY['pfmdr1_86']
WHERE key = 'crt_mdr1'  and col_type = 'DRUG';
