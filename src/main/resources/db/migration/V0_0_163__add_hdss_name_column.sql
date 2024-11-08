
ALTER TABLE  IF EXISTS hdss.hdss_compounds
    ADD COLUMN  IF NOT EXISTS name  varchar DEFAULT nextval('hdss.hdss_compounds_seq');

