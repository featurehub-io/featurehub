-- apply changes
create table fh_port_strategy (
  id                            varchar(40) not null,
  fk_portfolio_id               varchar(40) not null,
  when_archived                 datetime(6),
  strategy_name                 varchar(150) not null,
  strategy                      json not null,
  code                          varchar(10) not null,
  fk_person_who_changed         varchar(40) not null,
  when_updated                  datetime(6) not null,
  when_created                  datetime(6) not null,
  version                       bigint not null,
  constraint idx_pf_strategies unique (fk_portfolio_id,strategy_name),
  constraint idx_pf_strat_code unique (fk_portfolio_id,code),
  constraint pk_fh_port_strategy primary key (id)
);

create table fh_pstrat_for_feature (
  id                            varchar(40) not null,
  fk_fv_id                      varchar(40) not null,
  fk_prs_id                     varchar(40) not null,
  fv_enabled                    tinyint(1) default 0 not null,
  fv_value                      longtext,
  percent_oride                 integer,
  constraint idx_pfeature_strat unique (fk_fv_id,fk_prs_id),
  constraint pk_fh_pstrat_for_feature primary key (id)
);

-- apply alter tables
alter table fh_fv_version add column shared_pstrat json;
alter table fh_group add column p_roles json;
alter table fh_strat_for_feature add column percent_oride json;
-- foreign keys and indices
create index ix_fh_port_strategy_fk_portfolio_id on fh_port_strategy (fk_portfolio_id);
alter table fh_port_strategy add constraint fk_fh_port_strategy_fk_portfolio_id foreign key (fk_portfolio_id) references fh_portfolio (id) on delete restrict on update restrict;

create index ix_fh_port_strategy_fk_person_who_changed on fh_port_strategy (fk_person_who_changed);
alter table fh_port_strategy add constraint fk_fh_port_strategy_fk_person_who_changed foreign key (fk_person_who_changed) references fh_person (id) on delete restrict on update restrict;

create index ix_fh_pstrat_for_feature_fk_fv_id on fh_pstrat_for_feature (fk_fv_id);
alter table fh_pstrat_for_feature add constraint fk_fh_pstrat_for_feature_fk_fv_id foreign key (fk_fv_id) references fh_env_feature_strategy (id) on delete restrict on update restrict;

create index ix_fh_pstrat_for_feature_fk_prs_id on fh_pstrat_for_feature (fk_prs_id);
alter table fh_pstrat_for_feature add constraint fk_fh_pstrat_for_feature_fk_prs_id foreign key (fk_prs_id) references fh_port_strategy (id) on delete restrict on update restrict;
