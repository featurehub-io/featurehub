-- drop dependencies
-- apply changes
create table fh_celog (
  id                            uuid not null,
  fk_org                        uuid not null,
  link                          uuid not null,
  s                             integer,
  version                       bigint not null,
  when_upd                      timestamptz not null,
  when_cre                      timestamptz not null,
  type                          varchar(255) not null,
  link_type                     varchar(6) not null,
  data                          text not null,
  metadata                      text,
  te                            text,
  constraint pk_fh_celog primary key (id)
);

-- foreign keys and indices
create index ix_fh_celog_fk_org on fh_celog (fk_org);
alter table fh_celog add constraint fk_fh_celog_fk_org foreign key (fk_org) references fh_organization (id) on delete restrict on update restrict;

create index if not exists idx_cloudevents on fh_celog (type,link_type,link,when_upd);
create index if not exists idx_cloudevents_st on fh_celog (type,link_type,link,s);
create index if not exists idx_cloudevents_owner on fh_celog (id,fk_org,type);
