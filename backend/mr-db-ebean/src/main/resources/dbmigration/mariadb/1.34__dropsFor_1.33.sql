-- apply alter tables
alter table fh_organization drop constraint fk_fh_organization_fk_named_cache;
alter table fh_organization drop index ix_fh_organization_fk_named_cache;
alter table fh_organization drop column fk_named_cache;
-- apply post alter
drop table if exists fh_cache;
