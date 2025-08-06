-- apply alter tables
alter table fh_organization drop foreign key if exists fk_named_cache, drop index if exists fk_named_cache, drop column if exists fk_named_cache;
-- apply post alter
drop table if exists fh_cache;
