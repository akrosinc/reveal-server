create schema IF NOT EXISTS hdss;

CREATE TABLE IF NOT EXISTS hdss.hdss_compounds
(
    id            uuid              not null,
    compound_id   character varying not null,
    household_id  character varying not null,
    individual_id character varying not null,
    structure_id  uuid              not null,
    fields        jsonb,
    primary key (id)
    );

create sequence hdss.hdss_compounds_seq
    as int
    minvalue 0;

ALTER TABLE  IF EXISTS hdss.hdss_compounds
    ADD COLUMN  IF NOT EXISTS server_version  INT DEFAULT nextval('hdss.hdss_compounds_seq');

