-- apply alter tables
drop index ix_fh_organization_fk_named_cache on fh_organization;
alter table fh_organization drop column fk_named_cache;
-- apply post alter
drop table if exists fh_cache;
