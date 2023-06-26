-- apply alter tables
alter table fh_service_account add column fk_changed_by uuid not null default '00000000-0000-0000-0000-000000000000';
update fh_service_account set fk_changed_by = fk_person_who_created;
alter table fh_service_account add column fk_sdk_person uuid not null default '00000000-0000-0000-0000-000000000000';
-- we don't actually want these, but we need them to be valid references
update fh_service_account set fk_sdk_person = fk_person_who_created;
-- now we need the job
insert into fh_after_mig_job(id, completed, job_name) values (2, false, 'allocate-service-account-persons');
-- foreign keys and indices
-- foreign keys and indices
create index ix_fh_service_account_fk_changed_by on fh_service_account (fk_changed_by);
alter table fh_service_account add constraint fk_fh_service_account_fk_changed_by foreign key (fk_changed_by) references fh_person (id) on delete restrict on update restrict;

alter table fh_service_account add constraint fk_fh_service_account_fk_sdk_person foreign key (fk_sdk_person) references fh_person (id) on delete restrict on update restrict;

