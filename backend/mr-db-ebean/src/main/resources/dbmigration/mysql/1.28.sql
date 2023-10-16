-- delete existing strategy contents
delete from fh_strat_for_feature;
delete from fh_app_strategy;
-- apply alter tables
alter table fh_app_strategy modify strategy_name varchar(150) not null;
alter table fh_app_strategy add column code varchar(255) not null;
-- apply post alter
alter table fh_app_strategy add constraint idx_app_strat_code unique  (fk_app_id,code);
