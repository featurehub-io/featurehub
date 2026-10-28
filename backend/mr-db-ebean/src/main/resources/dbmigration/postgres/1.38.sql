-- apply alter tables
alter table fh_fv_version add column if not exists fg_strat json;
