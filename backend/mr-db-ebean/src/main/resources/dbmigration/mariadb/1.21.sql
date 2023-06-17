-- drop dependencies
alter table fh_service_account drop index idx_service_name;
-- apply changes
update fh_service_account set name = '' where name is null;

update fh_service_account set description = '' where description is null;

update fh_service_account set fk_sdk_person = '' where fk_sdk_person is null;

update fh_service_account set api_key_client_eval = '' where api_key_client_eval is null;
-- apply alter tables
alter table fh_service_account modify name varchar(100) not null;
alter table fh_service_account modify description varchar(400) not null;
alter table fh_service_account modify fk_sdk_person varchar(40) not null;
alter table fh_service_account modify api_key_client_eval varchar(100) not null;
-- apply post alter
alter table fh_service_account add constraint idx_service_name unique  (fk_portfolio_id,name);
