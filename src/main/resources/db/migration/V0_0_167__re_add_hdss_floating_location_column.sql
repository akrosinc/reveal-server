
ALTER TABLE  IF EXISTS hdss.hdss_compounds
    ADD COLUMN  IF NOT EXISTS floating_location_id  varchar ;

ALTER TABLE  IF EXISTS hdss.hdss_compounds
    ADD COLUMN  IF NOT EXISTS floating_location_name  varchar ;

ALTER TABLE  IF EXISTS hdss.hdss_compounds
    ADD COLUMN  IF NOT EXISTS floating_location_geographic_level  varchar ;

