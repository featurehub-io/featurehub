-- apply changes
create table fh_sysconfig (
  code                           varchar(255) not null,
  org_id                        varchar(40) not null,
  fk_who_updated                varchar(40),
  vl                         longtext,
  w_upd                         datetime(6) not null,
  w_cre                         datetime(6) not null,
  version                       bigint not null,
  constraint pk_fh_sysconfig primary key (code,org_id)
);

-- foreign keys and indices
create index ix_fh_sysconfig_org_id on fh_sysconfig (org_id);
alter table fh_sysconfig add constraint fk_fh_sysconfig_org_id foreign key (org_id) references fh_organization (id) on delete restrict on update restrict;

create index ix_fh_sysconfig_fk_who_updated on fh_sysconfig (fk_who_updated);
alter table fh_sysconfig add constraint fk_fh_sysconfig_fk_who_updated foreign key (fk_who_updated) references fh_person (id) on delete restrict on update restrict;

