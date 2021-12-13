-- apply changes
alter table fh_environment add column when_unpublished timestamp;

alter table fh_service_account add column when_unpublished timestamp;

