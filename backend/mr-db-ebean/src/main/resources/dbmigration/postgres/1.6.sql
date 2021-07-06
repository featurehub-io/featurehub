-- apply changes
alter table fh_service_account alter column name type varchar(100) using name::varchar(100);
