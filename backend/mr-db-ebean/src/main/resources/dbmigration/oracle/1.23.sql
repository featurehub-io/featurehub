insert into fh_person(id,name,email,when_updated,when_created,when_archived,version) values ('00000000-0000-0000-0000-000000000000',
     'Migrator', 'migrator@internal.featurehub.io',timestamp '2023-06-18 14:38:56.427',timestamp '2023-06-18 14:38:56.427',timestamp '2023-06-18 14:38:56.427',1);

-- apply changes
update fh_env_feature_strategy set fk_who_updated = '00000000-0000-0000-0000-000000000000' where fk_who_updated is null;

update fh_env_feature_strategy set retired = 0 where retired is null;

update fh_fv_version set fk_who_updated = '00000000-0000-0000-0000-000000000000' where fk_who_updated is null;
alter table fh_env_feature_strategy drop constraint fk_fh_nv_ftr_strtgy_fk_wh_p_1;
alter table fh_fv_version drop constraint fk_fh_fv_vrsn_fk_wh_pdtd;

-- apply alter tables
alter table fh_env_feature_strategy modify fk_who_updated not null;
alter table fh_env_feature_strategy modify retired default 0;
alter table fh_env_feature_strategy modify retired not null;
alter table fh_fv_version modify fk_who_updated not null;

alter table fh_env_feature_strategy add constraint fk_fh_nv_ftr_strtgy_fk_wh_p_1 foreign key (fk_who_updated) references fh_person (id);
alter table fh_fv_version add constraint fk_fh_fv_vrsn_fk_wh_pdtd foreign key (fk_who_updated) references fh_person (id);
