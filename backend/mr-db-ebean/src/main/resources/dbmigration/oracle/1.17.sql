-- apply changes
create table fh_fv_version (
  id                            varchar2(40) not null,
  version                       number(19) not null,
  fk_who_updated                varchar2(40),
  feature_state                 varchar2(8),
  default_value                 clob,
  locked                        number(1) default 0 not null,
  rollout_strat                 clob,
  retired                       number(1) default 0 not null,
  shared_strat                  clob,
  when_created                  timestamp not null,
  constraint ck_fh_fv_version_feature_state check ( feature_state in ('DISABLED','READY','ENABLED')),
  constraint pk_fh_fv_version primary key (id,version)
);

-- foreign keys and indices
create index ix_fh_fv_vrsn_fk_wh_pdtd on fh_fv_version (fk_who_updated);
alter table fh_fv_version add constraint fk_fh_fv_vrsn_fk_wh_pdtd foreign key (fk_who_updated) references fh_person (id) on delete set null;

