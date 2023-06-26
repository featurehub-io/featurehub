-- apply changes
update fh_app_feature set value_type = 'STRING' where value_type is null;
-- apply alter tables
alter table fh_app_feature modify value_type varchar(7) not null;
alter table fh_fv_version add column v_from bigint;
