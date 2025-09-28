
alter table if exists hdss.hdss_compounds
alter column compound_id drop not null;

alter table if exists hdss.hdss_compounds
alter column structure_id drop not null;

alter table if exists hdss.hdss_compounds
    add if not exists floating_household_location_name varchar;
