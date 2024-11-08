
ALTER TABLE IF EXISTS hdss.hdss_compounds
DROP COLUMN IF EXISTS name;

ALTER TABLE  IF EXISTS hdss.hdss_compounds
    ADD COLUMN  IF NOT EXISTS name  varchar ;

