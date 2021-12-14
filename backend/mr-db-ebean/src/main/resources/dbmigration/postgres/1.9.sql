-- apply changes
alter table fh_environment add column when_unpublished timestamptz;

alter table fh_service_account add column when_unpublished timestamptz;

