-- apply changes
create table fh_acl (
  id                            uuid not null,
  environment_id                uuid,
  application_id                uuid,
  group_id                      uuid,
  roles                         varchar(255),
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       bigint not null,
  constraint pk_fh_acl primary key (id)
);

create table fh_application (
  id                            uuid not null,
  name                          varchar(100) not null,
  description                   varchar(400),
  fk_person_who_created         uuid not null,
  fk_portfolio_id               uuid not null,
  when_archived                 timestamp,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       bigint not null,
  constraint pk_fh_application primary key (id)
);

create table fh_app_feature (
  id                            uuid not null,
  fk_app_id                     uuid not null,
  when_archived                 timestamp,
  feature_key                   varchar(255),
  alias                         varchar(255),
  name                          varchar(255),
  secret                        boolean default false not null,
  link                          varchar(600),
  value_type                    varchar(7),
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       bigint not null,
  constraint ck_fh_app_feature_value_type check ( value_type in ('BOOLEAN','STRING','NUMBER','JSON')),
  constraint idx_app_features unique (fk_app_id,feature_key),
  constraint pk_fh_app_feature primary key (id)
);

create table fh_environment (
  id                            uuid not null,
  is_prod_environment           boolean default false not null,
  fk_prior_env_id               uuid,
  fk_app_id                     uuid not null,
  name                          varchar(150) not null,
  description                   varchar(400),
  when_archived                 timestamp,
  when_unpublished              timestamp,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       bigint not null,
  constraint pk_fh_environment primary key (id)
);

create table fh_env_feature_strategy (
  id                            uuid not null,
  fk_who_updated                uuid,
  what_updated                  varchar(400),
  fk_environment_id             uuid not null,
  fk_feature_id                 uuid not null,
  feature_state                 varchar(8),
  default_value                 clob,
  locked                        boolean default false not null,
  rollout_strat                 clob,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       bigint not null,
  constraint ck_fh_env_feature_strategy_feature_state check ( feature_state in ('DISABLED','READY','ENABLED')),
  constraint pk_fh_env_feature_strategy primary key (id)
);

create table fh_group (
  id                            uuid not null,
  when_archived                 timestamp,
  fk_person_who_created         uuid not null,
  fk_portfolio_id               uuid,
  is_admin_group                boolean default false not null,
  fk_organization_id            uuid,
  group_name                    varchar(255),
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       bigint not null,
  constraint idx_group_names unique (fk_portfolio_id,group_name),
  constraint pk_fh_group primary key (id)
);

create table fh_login (
  token                         varchar(255) not null,
  person_id                     uuid,
  last_seen                     timestamp,
  constraint pk_fh_login primary key (token)
);

create table fh_cache (
  cache_name                    varchar(255) not null,
  constraint pk_fh_cache primary key (cache_name)
);

create table fh_organization (
  id                            uuid not null,
  when_archived                 timestamp,
  name                          varchar(255),
  fk_named_cache                varchar(255),
  group_id                      uuid,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       bigint not null,
  constraint uq_fh_organization_group_id unique (group_id),
  constraint pk_fh_organization primary key (id)
);

create table fh_person (
  id                            uuid not null,
  password_alg                  varchar(60) default 'PBKDF2WithHmacSHA1' not null,
  when_last_authenticated       timestamp,
  name                          varchar(100),
  email                         varchar(100) not null,
  password                      varchar(255),
  password_requires_reset       boolean default false not null,
  token                         varchar(255),
  token_expiry                  timestamp,
  who_changed_id                uuid,
  when_archived                 timestamp,
  fk_person_who_created         uuid,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       bigint not null,
  constraint idx_person_email unique (email),
  constraint pk_fh_person primary key (id)
);

create table fh_person_group_link (
  fk_person_id                  uuid not null,
  fk_group_id                   uuid not null,
  constraint pk_fh_person_group_link primary key (fk_person_id,fk_group_id)
);

create table fh_portfolio (
  id                            uuid not null,
  when_archived                 timestamp,
  fk_person_who_created         uuid,
  fk_org_id                     uuid not null,
  name                          varchar(255),
  description                   varchar(255),
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       bigint not null,
  constraint idx_portfolio_name unique (name,fk_org_id),
  constraint pk_fh_portfolio primary key (id)
);

create table fh_app_strategy (
  id                            uuid not null,
  fk_app_id                     uuid not null,
  when_archived                 timestamp,
  strategy_name                 varchar(255) not null,
  strategy                      clob not null,
  fk_person_who_changed         uuid not null,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       bigint not null,
  constraint idx_app_strategies unique (fk_app_id,strategy_name),
  constraint pk_fh_app_strategy primary key (id)
);

create table fh_service_account (
  id                            uuid not null,
  name                          varchar(100),
  description                   varchar(400),
  fk_person_who_created         uuid not null,
  api_key                       varchar(100) not null,
  api_key_client_eval           varchar(100),
  when_archived                 timestamp,
  fk_portfolio_id               uuid not null,
  when_unpublished              timestamp,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       bigint not null,
  constraint uq_fh_service_account_api_key unique (api_key),
  constraint uq_fh_service_account_api_key_client_eval unique (api_key_client_eval),
  constraint idx_service_name unique (fk_portfolio_id,name),
  constraint pk_fh_service_account primary key (id)
);

create table fh_service_account_env (
  id                            uuid not null,
  fk_environment_id             uuid not null,
  permissions                   varchar(200),
  fk_service_account_id         uuid not null,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       bigint not null,
  constraint pk_fh_service_account_env primary key (id)
);

create table fh_strat_for_feature (
  id                            uuid not null,
  fk_fv_id                      uuid not null,
  fk_rs_id                      uuid not null,
  fv_enabled                    boolean default false not null,
  fv_value                      clob,
  constraint idx_feature_strat unique (fk_fv_id,fk_rs_id),
  constraint pk_fh_strat_for_feature primary key (id)
);

create table fh_userstate (
  id                            uuid not null,
  fk_person                     uuid not null,
  fk_portfolio_id               uuid,
  fk_app_id                     uuid,
  fk_env_id                     uuid,
  user_state                    varchar(15),
  data                          clob,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       bigint not null,
  constraint ck_fh_userstate_user_state check ( user_state in ('HIDDEN_FEATURES')),
  constraint idx_user_state unique (fk_person,fk_portfolio_id,fk_app_id,fk_env_id),
  constraint pk_fh_userstate primary key (id)
);

create index ix_fh_acl_environment_id on fh_acl (environment_id);
alter table fh_acl add constraint fk_fh_acl_environment_id foreign key (environment_id) references fh_environment (id) on delete restrict on update restrict;

create index ix_fh_acl_application_id on fh_acl (application_id);
alter table fh_acl add constraint fk_fh_acl_application_id foreign key (application_id) references fh_application (id) on delete restrict on update restrict;

create index ix_fh_acl_group_id on fh_acl (group_id);
alter table fh_acl add constraint fk_fh_acl_group_id foreign key (group_id) references fh_group (id) on delete restrict on update restrict;

create index ix_fh_application_fk_person_who_created on fh_application (fk_person_who_created);
alter table fh_application add constraint fk_fh_application_fk_person_who_created foreign key (fk_person_who_created) references fh_person (id) on delete restrict on update restrict;

create index ix_fh_application_fk_portfolio_id on fh_application (fk_portfolio_id);
alter table fh_application add constraint fk_fh_application_fk_portfolio_id foreign key (fk_portfolio_id) references fh_portfolio (id) on delete restrict on update restrict;

create index ix_fh_app_feature_fk_app_id on fh_app_feature (fk_app_id);
alter table fh_app_feature add constraint fk_fh_app_feature_fk_app_id foreign key (fk_app_id) references fh_application (id) on delete restrict on update restrict;

create index ix_fh_environment_fk_prior_env_id on fh_environment (fk_prior_env_id);
alter table fh_environment add constraint fk_fh_environment_fk_prior_env_id foreign key (fk_prior_env_id) references fh_environment (id) on delete set null on update restrict;

create index ix_fh_environment_fk_app_id on fh_environment (fk_app_id);
alter table fh_environment add constraint fk_fh_environment_fk_app_id foreign key (fk_app_id) references fh_application (id) on delete restrict on update restrict;

create index ix_fh_env_feature_strategy_fk_who_updated on fh_env_feature_strategy (fk_who_updated);
alter table fh_env_feature_strategy add constraint fk_fh_env_feature_strategy_fk_who_updated foreign key (fk_who_updated) references fh_person (id) on delete set null on update restrict;

create index ix_fh_env_feature_strategy_fk_environment_id on fh_env_feature_strategy (fk_environment_id);
alter table fh_env_feature_strategy add constraint fk_fh_env_feature_strategy_fk_environment_id foreign key (fk_environment_id) references fh_environment (id) on delete restrict on update restrict;

create index ix_fh_env_feature_strategy_fk_feature_id on fh_env_feature_strategy (fk_feature_id);
alter table fh_env_feature_strategy add constraint fk_fh_env_feature_strategy_fk_feature_id foreign key (fk_feature_id) references fh_app_feature (id) on delete restrict on update restrict;

create index ix_fh_group_fk_person_who_created on fh_group (fk_person_who_created);
alter table fh_group add constraint fk_fh_group_fk_person_who_created foreign key (fk_person_who_created) references fh_person (id) on delete restrict on update restrict;

create index ix_fh_group_fk_portfolio_id on fh_group (fk_portfolio_id);
alter table fh_group add constraint fk_fh_group_fk_portfolio_id foreign key (fk_portfolio_id) references fh_portfolio (id) on delete restrict on update restrict;

create index ix_fh_group_fk_organization_id on fh_group (fk_organization_id);
alter table fh_group add constraint fk_fh_group_fk_organization_id foreign key (fk_organization_id) references fh_organization (id) on delete restrict on update restrict;

create index ix_fh_login_person_id on fh_login (person_id);
alter table fh_login add constraint fk_fh_login_person_id foreign key (person_id) references fh_person (id) on delete restrict on update restrict;

create index ix_fh_organization_fk_named_cache on fh_organization (fk_named_cache);
alter table fh_organization add constraint fk_fh_organization_fk_named_cache foreign key (fk_named_cache) references fh_cache (cache_name) on delete set null on update restrict;

alter table fh_organization add constraint fk_fh_organization_group_id foreign key (group_id) references fh_group (id) on delete restrict on update restrict;

create index ix_fh_person_who_changed_id on fh_person (who_changed_id);
alter table fh_person add constraint fk_fh_person_who_changed_id foreign key (who_changed_id) references fh_person (id) on delete restrict on update restrict;

create index ix_fh_person_fk_person_who_created on fh_person (fk_person_who_created);
alter table fh_person add constraint fk_fh_person_fk_person_who_created foreign key (fk_person_who_created) references fh_person (id) on delete restrict on update restrict;

create index ix_fh_person_group_link_fh_person on fh_person_group_link (fk_person_id);
alter table fh_person_group_link add constraint fk_fh_person_group_link_fh_person foreign key (fk_person_id) references fh_person (id) on delete restrict on update restrict;

create index ix_fh_person_group_link_fh_group on fh_person_group_link (fk_group_id);
alter table fh_person_group_link add constraint fk_fh_person_group_link_fh_group foreign key (fk_group_id) references fh_group (id) on delete restrict on update restrict;

create index ix_fh_portfolio_fk_person_who_created on fh_portfolio (fk_person_who_created);
alter table fh_portfolio add constraint fk_fh_portfolio_fk_person_who_created foreign key (fk_person_who_created) references fh_person (id) on delete restrict on update restrict;

create index ix_fh_portfolio_fk_org_id on fh_portfolio (fk_org_id);
alter table fh_portfolio add constraint fk_fh_portfolio_fk_org_id foreign key (fk_org_id) references fh_organization (id) on delete restrict on update restrict;

create index ix_fh_app_strategy_fk_app_id on fh_app_strategy (fk_app_id);
alter table fh_app_strategy add constraint fk_fh_app_strategy_fk_app_id foreign key (fk_app_id) references fh_application (id) on delete restrict on update restrict;

create index ix_fh_app_strategy_fk_person_who_changed on fh_app_strategy (fk_person_who_changed);
alter table fh_app_strategy add constraint fk_fh_app_strategy_fk_person_who_changed foreign key (fk_person_who_changed) references fh_person (id) on delete restrict on update restrict;

create index ix_fh_service_account_fk_person_who_created on fh_service_account (fk_person_who_created);
alter table fh_service_account add constraint fk_fh_service_account_fk_person_who_created foreign key (fk_person_who_created) references fh_person (id) on delete restrict on update restrict;

create index ix_fh_service_account_fk_portfolio_id on fh_service_account (fk_portfolio_id);
alter table fh_service_account add constraint fk_fh_service_account_fk_portfolio_id foreign key (fk_portfolio_id) references fh_portfolio (id) on delete restrict on update restrict;

create index ix_fh_service_account_env_fk_environment_id on fh_service_account_env (fk_environment_id);
alter table fh_service_account_env add constraint fk_fh_service_account_env_fk_environment_id foreign key (fk_environment_id) references fh_environment (id) on delete restrict on update restrict;

create index ix_fh_service_account_env_fk_service_account_id on fh_service_account_env (fk_service_account_id);
alter table fh_service_account_env add constraint fk_fh_service_account_env_fk_service_account_id foreign key (fk_service_account_id) references fh_service_account (id) on delete restrict on update restrict;

create index ix_fh_strat_for_feature_fk_fv_id on fh_strat_for_feature (fk_fv_id);
alter table fh_strat_for_feature add constraint fk_fh_strat_for_feature_fk_fv_id foreign key (fk_fv_id) references fh_env_feature_strategy (id) on delete restrict on update restrict;

create index ix_fh_strat_for_feature_fk_rs_id on fh_strat_for_feature (fk_rs_id);
alter table fh_strat_for_feature add constraint fk_fh_strat_for_feature_fk_rs_id foreign key (fk_rs_id) references fh_app_strategy (id) on delete restrict on update restrict;

create index ix_fh_userstate_fk_person on fh_userstate (fk_person);
alter table fh_userstate add constraint fk_fh_userstate_fk_person foreign key (fk_person) references fh_person (id) on delete restrict on update restrict;

create index ix_fh_userstate_fk_portfolio_id on fh_userstate (fk_portfolio_id);
alter table fh_userstate add constraint fk_fh_userstate_fk_portfolio_id foreign key (fk_portfolio_id) references fh_portfolio (id) on delete restrict on update restrict;

create index ix_fh_userstate_fk_app_id on fh_userstate (fk_app_id);
alter table fh_userstate add constraint fk_fh_userstate_fk_app_id foreign key (fk_app_id) references fh_application (id) on delete restrict on update restrict;

create index ix_fh_userstate_fk_env_id on fh_userstate (fk_env_id);
alter table fh_userstate add constraint fk_fh_userstate_fk_env_id foreign key (fk_env_id) references fh_environment (id) on delete restrict on update restrict;

