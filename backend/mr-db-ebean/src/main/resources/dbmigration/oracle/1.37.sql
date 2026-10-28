-- apply alter tables
alter table fh_pstrat_for_feature add sort_order number(10) default 0 not null;
alter table fh_strat_for_feature add sort_order number(10) default 0 not null;
