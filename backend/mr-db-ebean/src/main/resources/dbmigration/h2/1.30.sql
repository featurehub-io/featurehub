-- apply alter tables
alter table fh_environment add column w_env_inf clob;
-- insert into migration job
insert into fh_after_mig_job(id, completed, job_name) values (4, false, 'migrate-webhook-env-info');
