-- apply alter tables
alter table fh_organization drop column fk_named_cache;
-- apply post alter
drop table fh_cache cascade constraints purge;
