-- apply alter tables
alter table fh_pstrat_for_feature add column if not exists sort_order integer default 0 not null;
alter table fh_strat_for_feature add column if not exists sort_order integer default 0 not null;
