-- apply changes
create table fh_app_strategy (
  id                            uniqueidentifier not null,
  fk_app_id                     uniqueidentifier not null,
  when_archived                 datetime2,
  strategy_name                 nvarchar(255) not null,
  strategy                      nvarchar(max) not null,
  fk_person_who_changed         uniqueidentifier not null,
  version                       numeric(19) not null,
  when_updated                  datetime2 not null,
  when_created                  datetime2 not null,
  constraint pk_fh_app_strategy primary key (id)
);
alter table fh_app_strategy add constraint idx_app_strategies unique  (fk_app_id,strategy_name);

create table fh_strat_for_feature (
  id                            uniqueidentifier not null,
  fk_fv_id                      uniqueidentifier not null,
  fk_rs_id                      uniqueidentifier not null,
  enabled                       bit default 0 not null,
  value                         nvarchar(max),
  constraint pk_fh_strat_for_feature primary key (id)
);
alter table fh_strat_for_feature add constraint idx_feature_strat unique  (fk_fv_id,fk_rs_id);

create index ix_fh_app_strategy_fk_app_id on fh_app_strategy (fk_app_id);
alter table fh_app_strategy add constraint fk_fh_app_strategy_fk_app_id foreign key (fk_app_id) references fh_application (id);

create index ix_fh_app_strategy_fk_person_who_changed on fh_app_strategy (fk_person_who_changed);
alter table fh_app_strategy add constraint fk_fh_app_strategy_fk_person_who_changed foreign key (fk_person_who_changed) references fh_person (id);

create index ix_fh_strat_for_feature_fk_fv_id on fh_strat_for_feature (fk_fv_id);
alter table fh_strat_for_feature add constraint fk_fh_strat_for_feature_fk_fv_id foreign key (fk_fv_id) references fh_env_feature_strategy (id);

create index ix_fh_strat_for_feature_fk_rs_id on fh_strat_for_feature (fk_rs_id);
alter table fh_strat_for_feature add constraint fk_fh_strat_for_feature_fk_rs_id foreign key (fk_rs_id) references fh_app_strategy (id);

