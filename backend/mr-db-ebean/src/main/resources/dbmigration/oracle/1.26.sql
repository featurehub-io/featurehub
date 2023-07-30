-- apply changes
create table fh_featgroup (
  id                            varchar2(40) not null,
  gp_order                      number(10) not null,
  name                          varchar2(255) not null,
  dscr                          varchar2(255),
  fk_environment_id             varchar2(40) not null,
  strategies                    clob,
  when_archived                 timestamp,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       number(19) not null,
  constraint pk_fh_featgroup primary key (id)
);

create table fh_fg_feat (
  fk_feat_id                    varchar2(40) not null,
  fk_fg_id                      varchar2(40) not null,
  v                             clob,
  constraint pk_fh_fg_feat primary key (fk_feat_id,fk_fg_id)
);

-- foreign keys and indices
create index ix_fh_ftgrp_fk_nvrnmnt_d on fh_featgroup (fk_environment_id);
alter table fh_featgroup add constraint fk_fh_ftgrp_fk_nvrnmnt_d foreign key (fk_environment_id) references fh_environment (id);

create index ix_fh_fg_feat_fk_feat_id on fh_fg_feat (fk_feat_id);
alter table fh_fg_feat add constraint fk_fh_fg_feat_fk_feat_id foreign key (fk_feat_id) references fh_app_feature (id);

create index ix_fh_fg_feat_fk_fg_id on fh_fg_feat (fk_fg_id);
alter table fh_fg_feat add constraint fk_fh_fg_feat_fk_fg_id foreign key (fk_fg_id) references fh_featgroup (id);
