-- apply changes
create table fh_fv_version (
  id                            uuid not null,
  version                       bigint not null,
  fk_who_updated                uuid,
  feature_state                 varchar(8),
  default_value                 clob,
  locked                        boolean default false not null,
  rollout_strat                 clob,
  retired                       boolean default false not null,
  shared_strat                  clob,
  when_created                  timestamp not null,
  constraint ck_fh_fv_version_feature_state check ( feature_state in ('DISABLED','READY','ENABLED')),
  constraint pk_fh_fv_version primary key (id,version)
);

-- foreign keys and indices
create index ix_fh_fv_version_fk_who_updated on fh_fv_version (fk_who_updated);
alter table fh_fv_version add constraint fk_fh_fv_version_fk_who_updated foreign key (fk_who_updated) references fh_person (id) on delete set null on update restrict;

