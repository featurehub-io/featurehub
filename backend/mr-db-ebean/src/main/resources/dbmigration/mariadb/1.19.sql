-- apply changes
create table fh_featgroup (
  id                            varchar(40) not null,
  gp_order                      integer not null,
  name                          varchar(255) not null,
  dscr                          varchar(255),
  fk_environment_id             varchar(40) not null,
  strategies                    json,
  when_archived                 datetime(6),
  when_updated                  datetime(6) not null,
  when_created                  datetime(6) not null,
  version                       bigint not null,
  constraint pk_fh_featgroup primary key (id)
);

create table fh_fg_feat (
  fk_feat_id                    varchar(40) not null,
  fk_fg_id                      varchar(40) not null,
  v                             longtext,
  constraint pk_fh_fg_feat primary key (fk_feat_id,fk_fg_id)
);

-- foreign keys and indices
create index ix_fh_featgroup_fk_environment_id on fh_featgroup (fk_environment_id);
alter table fh_featgroup add constraint fk_fh_featgroup_fk_environment_id foreign key (fk_environment_id) references fh_environment (id) on delete restrict on update restrict;

create index ix_fh_fg_feat_fk_feat_id on fh_fg_feat (fk_feat_id);
alter table fh_fg_feat add constraint fk_fh_fg_feat_fk_feat_id foreign key (fk_feat_id) references fh_app_feature (id) on delete restrict on update restrict;

create index ix_fh_fg_feat_fk_fg_id on fh_fg_feat (fk_fg_id);
alter table fh_fg_feat add constraint fk_fh_fg_feat_fk_fg_id foreign key (fk_fg_id) references fh_featgroup (id) on delete restrict on update restrict;

