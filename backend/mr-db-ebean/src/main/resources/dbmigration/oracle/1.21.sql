-- drop dependencies
delimiter $$
declare
  expected_error exception;
  pragma exception_init(expected_error, -2443);
begin
  execute immediate 'alter table fh_service_account drop constraint idx_service_name';
exception
  when expected_error then null;
end;
$$;
-- apply changes
update fh_service_account set name = '' where name is null;

update fh_service_account set description = '' where description is null;

update fh_service_account set fk_sdk_person = '' where fk_sdk_person is null;

update fh_service_account set api_key_client_eval = '' where api_key_client_eval is null;
-- apply alter tables
alter table fh_service_account modify name not null;
alter table fh_service_account modify description not null;
alter table fh_service_account modify fk_sdk_person not null;
alter table fh_service_account modify api_key_client_eval not null;
-- apply post alter
alter table fh_service_account add constraint idx_service_name unique  (fk_portfolio_id,name);
