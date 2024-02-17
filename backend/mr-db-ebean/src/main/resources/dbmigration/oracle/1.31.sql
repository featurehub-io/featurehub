-- drop dependencies
-- apply changes
create table fh_celog (
  id                            varchar2(40) not null,
  fk_org                        varchar2(40) not null,
  type                          varchar2(255) not null,
  link_type                     varchar2(6) not null,
  link                          varchar2(40) not null,
  data                          clob not null,
  metadata                      clob,
  te                            clob,
  s                             number(10),
  version                       number(19) not null,
  when_upd                      timestamp not null,
  when_cre                      timestamp not null,
  constraint pk_fh_celog primary key (id)
);

-- foreign keys and indices
create index ix_fh_celog_fk_org on fh_celog (fk_org);
alter table fh_celog add constraint fk_fh_celog_fk_org foreign key (fk_org) references fh_organization (id);

create index idx_cloudevents on fh_celog (type,link_type,link,when_upd);
create index idx_cloudevents_st on fh_celog (type,link_type,link,s);
create index idx_cloudevents_owner on fh_celog (id,fk_org,type);
