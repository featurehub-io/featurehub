-- apply changes
alter table fh_strat_for_feature rename column enabled TO fv_enabled;
alter table fh_strat_for_feature rename column value to fv_value;

