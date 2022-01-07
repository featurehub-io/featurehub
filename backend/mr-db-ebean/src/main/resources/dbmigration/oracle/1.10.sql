-- apply changes
create table fh_acl (
  id                            varchar2(40) not null,
  environment_id                varchar2(40),
  application_id                varchar2(40),
  group_id                      varchar2(40),
  roles                         varchar2(255),
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       number(19) not null,
  constraint pk_fh_acl primary key (id)
);

create table fh_application (
  id                            varchar2(40) not null,
  name                          varchar2(100) not null,
  description                   varchar2(400),
  fk_person_who_created         varchar2(40) not null,
  fk_portfolio_id               varchar2(40) not null,
  when_archived                 timestamp,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       number(19) not null,
  constraint pk_fh_application primary key (id)
);

create table fh_app_feature (
  id                            varchar2(40) not null,
  fk_app_id                     varchar2(40) not null,
  when_archived                 timestamp,
  feature_key                   varchar2(255),
  alias                         varchar2(255),
  name                          varchar2(255),
  secret                        number(1) default 0 not null,
  link                          varchar2(600),
  value_type                    varchar2(7),
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       number(19) not null,
  constraint ck_fh_app_feature_value_type check ( value_type in ('BOOLEAN','STRING','NUMBER','JSON')),
  constraint idx_app_features unique (fk_app_id,feature_key),
  constraint pk_fh_app_feature primary key (id)
);

create table fh_environment (
  id                            varchar2(40) not null,
  is_prod_environment           number(1) default 0 not null,
  fk_prior_env_id               varchar2(40),
  fk_app_id                     varchar2(40) not null,
  name                          varchar2(150) not null,
  description                   varchar2(400),
  when_archived                 timestamp,
  when_unpublished              timestamp,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       number(19) not null,
  constraint pk_fh_environment primary key (id)
);

create table fh_env_feature_strategy (
  id                            varchar2(40) not null,
  fk_who_updated                varchar2(40),
  what_updated                  varchar2(400),
  fk_environment_id             varchar2(40) not null,
  fk_feature_id                 varchar2(40) not null,
  feature_state                 varchar2(8),
  default_value                 clob,
  locked                        number(1) default 0 not null,
  rollout_strat                 clob,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       number(19) not null,
  constraint ck_fh_nv_ftr_strtgy_ftr_stt check ( feature_state in ('DISABLED','READY','ENABLED')),
  constraint pk_fh_env_feature_strategy primary key (id)
);

create table fh_group (
  id                            varchar2(40) not null,
  when_archived                 timestamp,
  fk_person_who_created         varchar2(40) not null,
  fk_portfolio_id               varchar2(40),
  is_admin_group                number(1) default 0 not null,
  fk_organization_id            varchar2(40),
  group_name                    varchar2(255),
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       number(19) not null,
  constraint idx_group_names unique (fk_portfolio_id,group_name),
  constraint pk_fh_group primary key (id)
);

create table fh_login (
  token                         varchar2(255) not null,
  person_id                     varchar2(40),
  last_seen                     timestamp,
  constraint pk_fh_login primary key (token)
);

create table fh_cache (
  cache_name                    varchar2(255) not null,
  constraint pk_fh_cache primary key (cache_name)
);

create table fh_organization (
  id                            varchar2(40) not null,
  when_archived                 timestamp,
  name                          varchar2(255),
  fk_named_cache                varchar2(255),
  group_id                      varchar2(40),
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       number(19) not null,
  constraint uq_fh_organization_group_id unique (group_id),
  constraint pk_fh_organization primary key (id)
);

create table fh_person (
  id                            varchar2(40) not null,
  password_alg                  varchar2(60) default 'PBKDF2WithHmacSHA1' not null,
  when_last_authenticated       timestamp,
  name                          varchar2(100),
  email                         varchar2(100) not null,
  password                      varchar2(255),
  password_requires_reset       number(1) default 0 not null,
  token                         varchar2(255),
  token_expiry                  timestamp,
  who_changed_id                varchar2(40),
  when_archived                 timestamp,
  fk_person_who_created         varchar2(40),
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       number(19) not null,
  constraint idx_person_email unique (email),
  constraint pk_fh_person primary key (id)
);

create table fh_person_group_link (
  fk_person_id                  varchar2(40) not null,
  fk_group_id                   varchar2(40) not null,
  constraint pk_fh_person_group_link primary key (fk_person_id,fk_group_id)
);

create table fh_portfolio (
  id                            varchar2(40) not null,
  when_archived                 timestamp,
  fk_person_who_created         varchar2(40),
  fk_org_id                     varchar2(40) not null,
  name                          varchar2(255),
  description                   varchar2(255),
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       number(19) not null,
  constraint idx_portfolio_name unique (name,fk_org_id),
  constraint pk_fh_portfolio primary key (id)
);

create table fh_app_strategy (
  id                            varchar2(40) not null,
  fk_app_id                     varchar2(40) not null,
  when_archived                 timestamp,
  strategy_name                 varchar2(255) not null,
  strategy                      clob not null,
  fk_person_who_changed         varchar2(40) not null,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       number(19) not null,
  constraint idx_app_strategies unique (fk_app_id,strategy_name),
  constraint pk_fh_app_strategy primary key (id)
);

create table fh_service_account (
  id                            varchar2(40) not null,
  name                          varchar2(100),
  description                   varchar2(400),
  fk_person_who_created         varchar2(40) not null,
  api_key                       varchar2(100) not null,
  api_key_client_eval           varchar2(100),
  when_archived                 timestamp,
  fk_portfolio_id               varchar2(40) not null,
  when_unpublished              timestamp,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       number(19) not null,
  constraint uq_fh_service_account_api_key unique (api_key),
  constraint uq_fh_srvc_ccnt_p_ky_clnt_vl unique (api_key_client_eval),
  constraint idx_service_name unique (fk_portfolio_id,name),
  constraint pk_fh_service_account primary key (id)
);

create table fh_service_account_env (
  id                            varchar2(40) not null,
  fk_environment_id             varchar2(40) not null,
  permissions                   varchar2(200),
  fk_service_account_id         varchar2(40) not null,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       number(19) not null,
  constraint pk_fh_service_account_env primary key (id)
);

create table fh_strat_for_feature (
  id                            varchar2(40) not null,
  fk_fv_id                      varchar2(40) not null,
  fk_rs_id                      varchar2(40) not null,
  fv_enabled                    number(1) default 0 not null,
  fv_value                      clob,
  constraint idx_feature_strat unique (fk_fv_id,fk_rs_id),
  constraint pk_fh_strat_for_feature primary key (id)
);

create table fh_userstate (
  id                            varchar2(40) not null,
  fk_person                     varchar2(40) not null,
  fk_portfolio_id               varchar2(40),
  fk_app_id                     varchar2(40),
  fk_env_id                     varchar2(40),
  user_state                    varchar2(15),
  data                          clob,
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       number(19) not null,
  constraint ck_fh_userstate_user_state check ( user_state in ('HIDDEN_FEATURES')),
  constraint idx_user_state unique (fk_person,fk_portfolio_id,fk_app_id,fk_env_id),
  constraint pk_fh_userstate primary key (id)
);

create index ix_fh_acl_environment_id on fh_acl (environment_id);
alter table fh_acl add constraint fk_fh_acl_environment_id foreign key (environment_id) references fh_environment (id);

create index ix_fh_acl_application_id on fh_acl (application_id);
alter table fh_acl add constraint fk_fh_acl_application_id foreign key (application_id) references fh_application (id);

create index ix_fh_acl_group_id on fh_acl (group_id);
alter table fh_acl add constraint fk_fh_acl_group_id foreign key (group_id) references fh_group (id);

create index ix_fh_pplctn_fk_prsn_wh_crtd on fh_application (fk_person_who_created);
alter table fh_application add constraint fk_fh_pplctn_fk_prsn_wh_crtd foreign key (fk_person_who_created) references fh_person (id);

create index ix_fh_pplctn_fk_prtfl_d on fh_application (fk_portfolio_id);
alter table fh_application add constraint fk_fh_pplctn_fk_prtfl_d foreign key (fk_portfolio_id) references fh_portfolio (id);

create index ix_fh_app_feature_fk_app_id on fh_app_feature (fk_app_id);
alter table fh_app_feature add constraint fk_fh_app_feature_fk_app_id foreign key (fk_app_id) references fh_application (id);

create index ix_fh_nvrnmnt_fk_prr_nv_d on fh_environment (fk_prior_env_id);
alter table fh_environment add constraint fk_fh_nvrnmnt_fk_prr_nv_d foreign key (fk_prior_env_id) references fh_environment (id) on delete set null;

create index ix_fh_environment_fk_app_id on fh_environment (fk_app_id);
alter table fh_environment add constraint fk_fh_environment_fk_app_id foreign key (fk_app_id) references fh_application (id);

create index ix_fh_nv_ftr_strtgy_fk_wh_p_1 on fh_env_feature_strategy (fk_who_updated);
alter table fh_env_feature_strategy add constraint fk_fh_nv_ftr_strtgy_fk_wh_p_1 foreign key (fk_who_updated) references fh_person (id) on delete set null;

create index ix_fh_nv_ftr_strtgy_fk_nvrn_2 on fh_env_feature_strategy (fk_environment_id);
alter table fh_env_feature_strategy add constraint fk_fh_nv_ftr_strtgy_fk_nvrn_2 foreign key (fk_environment_id) references fh_environment (id);

create index ix_fh_nv_ftr_strtgy_fk_ftr_d on fh_env_feature_strategy (fk_feature_id);
alter table fh_env_feature_strategy add constraint fk_fh_nv_ftr_strtgy_fk_ftr_d foreign key (fk_feature_id) references fh_app_feature (id);

create index ix_fh_grp_fk_prsn_wh_crtd on fh_group (fk_person_who_created);
alter table fh_group add constraint fk_fh_grp_fk_prsn_wh_crtd foreign key (fk_person_who_created) references fh_person (id);

create index ix_fh_group_fk_portfolio_id on fh_group (fk_portfolio_id);
alter table fh_group add constraint fk_fh_group_fk_portfolio_id foreign key (fk_portfolio_id) references fh_portfolio (id);

create index ix_fh_grp_fk_rgnztn_d on fh_group (fk_organization_id);
alter table fh_group add constraint fk_fh_grp_fk_rgnztn_d foreign key (fk_organization_id) references fh_organization (id);

create index ix_fh_login_person_id on fh_login (person_id);
alter table fh_login add constraint fk_fh_login_person_id foreign key (person_id) references fh_person (id);

create index ix_fh_rgnztn_fk_nmd_cch on fh_organization (fk_named_cache);
alter table fh_organization add constraint fk_fh_rgnztn_fk_nmd_cch foreign key (fk_named_cache) references fh_cache (cache_name) on delete set null;

alter table fh_organization add constraint fk_fh_organization_group_id foreign key (group_id) references fh_group (id);

create index ix_fh_person_who_changed_id on fh_person (who_changed_id);
alter table fh_person add constraint fk_fh_person_who_changed_id foreign key (who_changed_id) references fh_person (id);

create index ix_fh_prsn_fk_prsn_wh_crtd on fh_person (fk_person_who_created);
alter table fh_person add constraint fk_fh_prsn_fk_prsn_wh_crtd foreign key (fk_person_who_created) references fh_person (id);

create index ix_fh_prsn_grp_lnk_fh_prsn on fh_person_group_link (fk_person_id);
alter table fh_person_group_link add constraint fk_fh_prsn_grp_lnk_fh_prsn foreign key (fk_person_id) references fh_person (id);

create index ix_fh_prsn_grp_lnk_fh_grp on fh_person_group_link (fk_group_id);
alter table fh_person_group_link add constraint fk_fh_prsn_grp_lnk_fh_grp foreign key (fk_group_id) references fh_group (id);

create index ix_fh_prtfl_fk_prsn_wh_crtd on fh_portfolio (fk_person_who_created);
alter table fh_portfolio add constraint fk_fh_prtfl_fk_prsn_wh_crtd foreign key (fk_person_who_created) references fh_person (id);

create index ix_fh_portfolio_fk_org_id on fh_portfolio (fk_org_id);
alter table fh_portfolio add constraint fk_fh_portfolio_fk_org_id foreign key (fk_org_id) references fh_organization (id);

create index ix_fh_app_strategy_fk_app_id on fh_app_strategy (fk_app_id);
alter table fh_app_strategy add constraint fk_fh_app_strategy_fk_app_id foreign key (fk_app_id) references fh_application (id);

create index ix_fh_pp_strtgy_fk_prsn_wh__2 on fh_app_strategy (fk_person_who_changed);
alter table fh_app_strategy add constraint fk_fh_pp_strtgy_fk_prsn_wh__2 foreign key (fk_person_who_changed) references fh_person (id);

create index ix_fh_srvc_ccnt_fk_prsn_wh__1 on fh_service_account (fk_person_who_created);
alter table fh_service_account add constraint fk_fh_srvc_ccnt_fk_prsn_wh__1 foreign key (fk_person_who_created) references fh_person (id);

create index ix_fh_srvc_ccnt_fk_prtfl_d on fh_service_account (fk_portfolio_id);
alter table fh_service_account add constraint fk_fh_srvc_ccnt_fk_prtfl_d foreign key (fk_portfolio_id) references fh_portfolio (id);

create index ix_fh_srvc_ccnt_nv_fk_nvrnm_1 on fh_service_account_env (fk_environment_id);
alter table fh_service_account_env add constraint fk_fh_srvc_ccnt_nv_fk_nvrnm_1 foreign key (fk_environment_id) references fh_environment (id);

create index ix_fh_srvc_ccnt_nv_fk_srvc__2 on fh_service_account_env (fk_service_account_id);
alter table fh_service_account_env add constraint fk_fh_srvc_ccnt_nv_fk_srvc__2 foreign key (fk_service_account_id) references fh_service_account (id);

create index ix_fh_strt_fr_ftr_fk_fv_d on fh_strat_for_feature (fk_fv_id);
alter table fh_strat_for_feature add constraint fk_fh_strt_fr_ftr_fk_fv_d foreign key (fk_fv_id) references fh_env_feature_strategy (id);

create index ix_fh_strt_fr_ftr_fk_rs_d on fh_strat_for_feature (fk_rs_id);
alter table fh_strat_for_feature add constraint fk_fh_strt_fr_ftr_fk_rs_d foreign key (fk_rs_id) references fh_app_strategy (id);

create index ix_fh_userstate_fk_person on fh_userstate (fk_person);
alter table fh_userstate add constraint fk_fh_userstate_fk_person foreign key (fk_person) references fh_person (id);

create index ix_fh_srstt_fk_prtfl_d on fh_userstate (fk_portfolio_id);
alter table fh_userstate add constraint fk_fh_srstt_fk_prtfl_d foreign key (fk_portfolio_id) references fh_portfolio (id);

create index ix_fh_userstate_fk_app_id on fh_userstate (fk_app_id);
alter table fh_userstate add constraint fk_fh_userstate_fk_app_id foreign key (fk_app_id) references fh_application (id);

create index ix_fh_userstate_fk_env_id on fh_userstate (fk_env_id);
alter table fh_userstate add constraint fk_fh_userstate_fk_env_id foreign key (fk_env_id) references fh_environment (id);

