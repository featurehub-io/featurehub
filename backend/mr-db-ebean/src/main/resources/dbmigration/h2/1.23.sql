insert into fh_person(id,name,email,when_updated,when_created,when_archived,version) values ('00000000-0000-0000-0000-000000000000',
         'Migrator', 'migrator@internal.featurehub.io','2023-06-18T14:38:56.427','2023-06-18T14:38:56.427','2023-06-18T14:38:56.427',1);

update fh_env_feature_strategy set fk_who_updated = (select id from fh_person where email='migrator@internal.featurehub.io') where fk_who_updated is null;

update fh_env_feature_strategy set retired = false where retired is null;

update fh_fv_version set fk_who_updated = (select id from fh_person where email='migrator@internal.featurehub.io') where fk_who_updated is null;
-- apply alter tables
alter table fh_env_feature_strategy alter column fk_who_updated set not null;
alter table fh_env_feature_strategy alter column retired set default false;
alter table fh_env_feature_strategy alter column retired set not null;
alter table fh_fv_version alter column fk_who_updated set not null;
