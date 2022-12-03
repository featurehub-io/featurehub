-- apply changes
create table fh_fv_version (
  id                            varchar(40) not null,
  version                       bigint not null,
  fk_who_updated                varchar(40),
  feature_state                 varchar(8),
  default_value                 longtext,
  locked                        tinyint(1) default 0 not null,
  rollout_strat                 json,
  retired                       tinyint(1) default 0 not null,
  shared_strat                  json,
  when_created                  datetime(6) not null,
  constraint pk_fh_fv_version primary key (id,version)
);

-- foreign keys and indices
create index ix_fh_fv_version_fk_who_updated on fh_fv_version (fk_who_updated);
alter table fh_fv_version add constraint fk_fh_fv_version_fk_who_updated foreign key (fk_who_updated) references fh_person (id) on delete set null on update restrict;

