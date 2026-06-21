-- apply changes
create table fh_port_strategy (
  id                            varchar2(40) not null,
  fk_portfolio_id               varchar2(40) not null,
  when_archived                 timestamp,
  strategy_name                 varchar2(150) not null,
  strategy                      clob not null,
  code                          varchar2(10) not null,
  fk_person_who_changed         varchar2(40) not null,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       number(19) not null,
  constraint idx_pf_strategies unique (fk_portfolio_id,strategy_name),
  constraint idx_pf_strat_code unique (fk_portfolio_id,code),
  constraint pk_fh_port_strategy primary key (id)
);

create table fh_pstrat_for_feature (
  id                            varchar2(40) not null,
  fk_fv_id                      varchar2(40) not null,
  fk_prs_id                     varchar2(40) not null,
  fv_enabled                    number(1) default 0 not null,
  fv_value                      clob,
  percent_oride                 number(10),
  constraint idx_pfeature_strat unique (fk_fv_id,fk_prs_id),
  constraint pk_fh_pstrat_for_feature primary key (id)
);

-- apply alter tables
alter table fh_fv_version add shared_pstrat clob;
alter table fh_group add p_roles varchar2(100);
alter table fh_strat_for_feature add percent_oride clob;
-- foreign keys and indices
create index ix_fh_prt_strtgy_fk_prtfl_d on fh_port_strategy (fk_portfolio_id);
alter table fh_port_strategy add constraint fk_fh_prt_strtgy_fk_prtfl_d foreign key (fk_portfolio_id) references fh_portfolio (id);

create index ix_fh_prt_strtgy_fk_prs_s4b9vs on fh_port_strategy (fk_person_who_changed);
alter table fh_port_strategy add constraint fk_fh_prt_strtgy_fk_prs_n0jofm foreign key (fk_person_who_changed) references fh_person (id);

create index ix_fh_pstrt_fr_ftr_fk_fv_d on fh_pstrat_for_feature (fk_fv_id);
alter table fh_pstrat_for_feature add constraint fk_fh_pstrt_fr_ftr_fk_fv_d foreign key (fk_fv_id) references fh_env_feature_strategy (id);

create index ix_fh_pstrt_fr_ftr_fk_prs_d on fh_pstrat_for_feature (fk_prs_id);
alter table fh_pstrat_for_feature add constraint fk_fh_pstrt_fr_ftr_fk_prs_d foreign key (fk_prs_id) references fh_port_strategy (id);

