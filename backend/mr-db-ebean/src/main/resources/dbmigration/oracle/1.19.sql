-- apply alter tables
alter table fh_service_account add column fk_changed_by varchar2(40) not null default '00000000-0000-0000-0000-000000000000';
update fh_service_account set fk_changed_by = fk_person_who_created;
alter table fh_service_account add column fk_sdk_person varchar2(40) not null default '00000000-0000-0000-0000-000000000000';
-- we don't actually want these, but we need them to be valid references
update fh_service_account set fk_sdk_person = fk_person_who_created;
-- now we need the job
insert into fh_after_mig_job(id, completed, job_name) values (2, 0, 'allocate-service-account-persons');
-- foreign keys and indices
-- foreign keys and indices
create index ix_fh_srvc_ccnt_fk_chngd_by on fh_service_account (fk_changed_by);
alter table fh_service_account add constraint fk_fh_srvc_ccnt_fk_chngd_by foreign key (fk_changed_by) references fh_person (id);

alter table fh_service_account add constraint fk_fh_srvc_ccnt_fk_sdk_prsn foreign key (fk_sdk_person) references fh_person (id);

