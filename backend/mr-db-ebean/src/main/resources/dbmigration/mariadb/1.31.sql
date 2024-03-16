-- drop dependencies
-- apply changes
create table fh_celog (
  id                            varchar(40) not null,
  fk_org                        varchar(40) not null,
  type                          varchar(255) not null,
  link_type                     varchar(6) not null,
  link                          varchar(40) not null,
  data                          longtext not null,
  metadata                      longtext,
  te                            longtext,
  s                             integer,
  version                       bigint not null,
  when_upd                      datetime(6) not null,
  when_cre                      datetime(6) not null,
  constraint pk_fh_celog primary key (id)
);

-- foreign keys and indices
create index ix_fh_celog_fk_org on fh_celog (fk_org);
alter table fh_celog add constraint fk_fh_celog_fk_org foreign key (fk_org) references fh_organization (id) on delete restrict on update restrict;

create index idx_cloudevents on fh_celog (type,link_type,link,when_upd);
create index idx_cloudevents_st on fh_celog (type,link_type,link,s);
create index idx_cloudevents_owner on fh_celog (id,fk_org,type);
