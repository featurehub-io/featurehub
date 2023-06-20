-- apply changes
create table fh_featgroup (
  id                            uuid not null,
  gp_order                      integer not null,
  fk_environment_id             uuid not null,
  when_archived                 timestamptz,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       bigint not null,
  name                          varchar(255) not null,
  dscr                          varchar(255),
  strategies                    json,
  constraint pk_fh_featgroup primary key (id)
);

create table fh_fg_feat (
  fk_feat_id                    uuid not null,
  fk_fg_id                      uuid not null,
  v                             text,
  constraint pk_fh_fg_feat primary key (fk_feat_id,fk_fg_id)
);

-- foreign keys and indices
create index ix_fh_featgroup_fk_environment_id on fh_featgroup (fk_environment_id);
alter table fh_featgroup add constraint fk_fh_featgroup_fk_environment_id foreign key (fk_environment_id) references fh_environment (id) on delete restrict on update restrict;

create index ix_fh_fg_feat_fk_feat_id on fh_fg_feat (fk_feat_id);
alter table fh_fg_feat add constraint fk_fh_fg_feat_fk_feat_id foreign key (fk_feat_id) references fh_app_feature (id) on delete restrict on update restrict;

create index ix_fh_fg_feat_fk_fg_id on fh_fg_feat (fk_fg_id);
alter table fh_fg_feat add constraint fk_fh_fg_feat_fk_fg_id foreign key (fk_fg_id) references fh_featgroup (id) on delete restrict on update restrict;

