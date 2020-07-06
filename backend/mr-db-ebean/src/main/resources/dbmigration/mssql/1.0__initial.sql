-- apply changes
create table fh_acl (
  id                            uniqueidentifier not null,
  environment_id                uniqueidentifier,
  application_id                uniqueidentifier,
  group_id                      uniqueidentifier,
  roles                         nvarchar(255),
  version                       numeric(19) not null,
  when_updated                  datetime2 not null,
  when_created                  datetime2 not null,
  constraint pk_fh_acl primary key (id)
);

create table fh_application (
  id                            uniqueidentifier not null,
  name                          nvarchar(100) not null,
  description                   nvarchar(400),
  fk_person_who_created         uniqueidentifier not null,
  fk_portfolio_id               uniqueidentifier not null,
  when_archived                 datetime2,
  version                       numeric(19) not null,
  when_updated                  datetime2 not null,
  when_created                  datetime2 not null,
  constraint pk_fh_application primary key (id)
);

create table fh_app_feature (
  id                            uniqueidentifier not null,
  fk_app_id                     uniqueidentifier not null,
  when_archived                 datetime2,
  feature_key                   nvarchar(255),
  alias                         nvarchar(255),
  name                          nvarchar(255),
  secret                        bit default 0 not null,
  link                          nvarchar(600),
  value_type                    nvarchar(7),
  version                       numeric(19) not null,
  when_updated                  datetime2 not null,
  when_created                  datetime2 not null,
  constraint ck_fh_app_feature_value_type check ( value_type in ('BOOLEAN','STRING','NUMBER','JSON')),
  constraint pk_fh_app_feature primary key (id)
);
create unique nonclustered index idx_app_features on fh_app_feature(fk_app_id,feature_key) where feature_key is not null;

create table fh_environment (
  id                            uniqueidentifier not null,
  is_prod_environment           bit default 0 not null,
  fk_prior_env_id               uniqueidentifier,
  fk_app_id                     uniqueidentifier not null,
  name                          nvarchar(150) not null,
  description                   nvarchar(400),
  when_archived                 datetime2,
  version                       numeric(19) not null,
  when_updated                  datetime2 not null,
  when_created                  datetime2 not null,
  constraint pk_fh_environment primary key (id)
);

create table fh_env_feature_strategy (
  id                            uniqueidentifier not null,
  fk_who_updated                uniqueidentifier,
  what_updated                  nvarchar(400),
  fk_environment_id             uniqueidentifier not null,
  fk_feature_id                 uniqueidentifier not null,
  feature_state                 nvarchar(8),
  default_value                 nvarchar(max),
  enabled_strategy              nvarchar(10),
  locked                        bit default 0 not null,
  rollout_strat                 nvarchar(max),
  version                       numeric(19) not null,
  when_updated                  datetime2 not null,
  when_created                  datetime2 not null,
  constraint ck_fh_env_feature_strategy_feature_state check ( feature_state in ('DISABLED','READY','ENABLED')),
  constraint ck_fh_env_feature_strategy_enabled_strategy check ( enabled_strategy in ('ATTRIBUTE','PERCENTAGE')),
  constraint pk_fh_env_feature_strategy primary key (id)
);

create table fh_group (
  id                            uniqueidentifier not null,
  when_archived                 datetime2,
  fk_person_who_created         uniqueidentifier not null,
  fk_portfolio_id               uniqueidentifier,
  is_admin_group                bit default 0 not null,
  fk_organization_id            uniqueidentifier,
  group_name                    nvarchar(255),
  when_updated                  datetime2 not null,
  when_created                  datetime2 not null,
  version                       numeric(19) not null,
  constraint pk_fh_group primary key (id)
);
create unique nonclustered index idx_group_names on fh_group(fk_portfolio_id,group_name) where fk_portfolio_id is not null and group_name is not null;

create table fh_login (
  token                         nvarchar(255) not null,
  person_id                     uniqueidentifier,
  last_seen                     datetime2,
  constraint pk_fh_login primary key (token)
);

create table fh_cache (
  cache_name                    nvarchar(255) not null,
  constraint pk_fh_cache primary key (cache_name)
);

create table fh_organization (
  id                            uniqueidentifier not null,
  when_archived                 datetime2,
  name                          nvarchar(255),
  fk_named_cache                nvarchar(255),
  group_id                      uniqueidentifier,
  version                       numeric(19) not null,
  when_updated                  datetime2 not null,
  when_created                  datetime2 not null,
  constraint pk_fh_organization primary key (id)
);
create unique nonclustered index uq_fh_organization_group_id on fh_organization(group_id) where group_id is not null;

create table fh_person (
  id                            uniqueidentifier not null,
  when_last_authenticated       datetime2,
  name                          nvarchar(100),
  email                         nvarchar(100) not null,
  password                      nvarchar(255),
  password_requires_reset       bit default 0 not null,
  token                         nvarchar(255),
  token_expiry                  datetime2,
  who_changed_id                uniqueidentifier,
  when_archived                 datetime2,
  fk_person_who_created         uniqueidentifier,
  version                       numeric(19) not null,
  when_updated                  datetime2 not null,
  when_created                  datetime2 not null,
  constraint pk_fh_person primary key (id)
);
alter table fh_person add constraint idx_person_email unique  (email);

create table fh_person_group_link (
  fk_person_id                  uniqueidentifier not null,
  fk_group_id                   uniqueidentifier not null,
  constraint pk_fh_person_group_link primary key (fk_person_id,fk_group_id)
);

create table fh_portfolio (
  id                            uniqueidentifier not null,
  when_archived                 datetime2,
  fk_person_who_created         uniqueidentifier not null,
  fk_org_id                     uniqueidentifier not null,
  name                          nvarchar(255),
  description                   nvarchar(255),
  when_updated                  datetime2 not null,
  when_created                  datetime2 not null,
  version                       numeric(19) not null,
  constraint pk_fh_portfolio primary key (id)
);
create unique nonclustered index idx_portfolio_name on fh_portfolio(name,fk_org_id) where name is not null;

create table fh_service_account (
  id                            uniqueidentifier not null,
  name                          nvarchar(40),
  description                   nvarchar(400),
  fk_person_who_created         uniqueidentifier not null,
  api_key                       nvarchar(100) not null,
  when_archived                 datetime2,
  fk_portfolio_id               uniqueidentifier not null,
  when_updated                  datetime2 not null,
  when_created                  datetime2 not null,
  version                       numeric(19) not null,
  constraint uq_fh_service_account_api_key unique (api_key),
  constraint pk_fh_service_account primary key (id)
);
create unique nonclustered index idx_service_name on fh_service_account(fk_portfolio_id,name) where name is not null;

create table fh_service_account_env (
  id                            uniqueidentifier not null,
  fk_environment_id             uniqueidentifier not null,
  permissions                   nvarchar(200),
  fk_service_account_id         uniqueidentifier not null,
  when_updated                  datetime2 not null,
  when_created                  datetime2 not null,
  version                       numeric(19) not null,
  constraint pk_fh_service_account_env primary key (id)
);

create index ix_fh_acl_environment_id on fh_acl (environment_id);
alter table fh_acl add constraint fk_fh_acl_environment_id foreign key (environment_id) references fh_environment (id);

create index ix_fh_acl_application_id on fh_acl (application_id);
alter table fh_acl add constraint fk_fh_acl_application_id foreign key (application_id) references fh_application (id);

create index ix_fh_acl_group_id on fh_acl (group_id);
alter table fh_acl add constraint fk_fh_acl_group_id foreign key (group_id) references fh_group (id);

create index ix_fh_application_fk_person_who_created on fh_application (fk_person_who_created);
alter table fh_application add constraint fk_fh_application_fk_person_who_created foreign key (fk_person_who_created) references fh_person (id);

create index ix_fh_application_fk_portfolio_id on fh_application (fk_portfolio_id);
alter table fh_application add constraint fk_fh_application_fk_portfolio_id foreign key (fk_portfolio_id) references fh_portfolio (id);

create index ix_fh_app_feature_fk_app_id on fh_app_feature (fk_app_id);
alter table fh_app_feature add constraint fk_fh_app_feature_fk_app_id foreign key (fk_app_id) references fh_application (id);

create index ix_fh_environment_fk_prior_env_id on fh_environment (fk_prior_env_id);
alter table fh_environment add constraint fk_fh_environment_fk_prior_env_id foreign key (fk_prior_env_id) references fh_environment (id) on delete set null on update set null;

create index ix_fh_environment_fk_app_id on fh_environment (fk_app_id);
alter table fh_environment add constraint fk_fh_environment_fk_app_id foreign key (fk_app_id) references fh_application (id);

create index ix_fh_env_feature_strategy_fk_who_updated on fh_env_feature_strategy (fk_who_updated);
alter table fh_env_feature_strategy add constraint fk_fh_env_feature_strategy_fk_who_updated foreign key (fk_who_updated) references fh_person (id) on delete set null on update set null;

create index ix_fh_env_feature_strategy_fk_environment_id on fh_env_feature_strategy (fk_environment_id);
alter table fh_env_feature_strategy add constraint fk_fh_env_feature_strategy_fk_environment_id foreign key (fk_environment_id) references fh_environment (id);

create index ix_fh_env_feature_strategy_fk_feature_id on fh_env_feature_strategy (fk_feature_id);
alter table fh_env_feature_strategy add constraint fk_fh_env_feature_strategy_fk_feature_id foreign key (fk_feature_id) references fh_app_feature (id);

create index ix_fh_group_fk_person_who_created on fh_group (fk_person_who_created);
alter table fh_group add constraint fk_fh_group_fk_person_who_created foreign key (fk_person_who_created) references fh_person (id);

create index ix_fh_group_fk_portfolio_id on fh_group (fk_portfolio_id);
alter table fh_group add constraint fk_fh_group_fk_portfolio_id foreign key (fk_portfolio_id) references fh_portfolio (id);

create index ix_fh_group_fk_organization_id on fh_group (fk_organization_id);
alter table fh_group add constraint fk_fh_group_fk_organization_id foreign key (fk_organization_id) references fh_organization (id);

create index ix_fh_login_person_id on fh_login (person_id);
alter table fh_login add constraint fk_fh_login_person_id foreign key (person_id) references fh_person (id);

create index ix_fh_organization_fk_named_cache on fh_organization (fk_named_cache);
alter table fh_organization add constraint fk_fh_organization_fk_named_cache foreign key (fk_named_cache) references fh_cache (cache_name) on delete set null on update set null;

alter table fh_organization add constraint fk_fh_organization_group_id foreign key (group_id) references fh_group (id);

create index ix_fh_person_who_changed_id on fh_person (who_changed_id);
alter table fh_person add constraint fk_fh_person_who_changed_id foreign key (who_changed_id) references fh_person (id);

create index ix_fh_person_fk_person_who_created on fh_person (fk_person_who_created);
alter table fh_person add constraint fk_fh_person_fk_person_who_created foreign key (fk_person_who_created) references fh_person (id);

create index ix_fh_person_group_link_fh_person on fh_person_group_link (fk_person_id);
alter table fh_person_group_link add constraint fk_fh_person_group_link_fh_person foreign key (fk_person_id) references fh_person (id);

create index ix_fh_person_group_link_fh_group on fh_person_group_link (fk_group_id);
alter table fh_person_group_link add constraint fk_fh_person_group_link_fh_group foreign key (fk_group_id) references fh_group (id);

create index ix_fh_portfolio_fk_person_who_created on fh_portfolio (fk_person_who_created);
alter table fh_portfolio add constraint fk_fh_portfolio_fk_person_who_created foreign key (fk_person_who_created) references fh_person (id);

create index ix_fh_portfolio_fk_org_id on fh_portfolio (fk_org_id);
alter table fh_portfolio add constraint fk_fh_portfolio_fk_org_id foreign key (fk_org_id) references fh_organization (id);

create index ix_fh_service_account_fk_person_who_created on fh_service_account (fk_person_who_created);
alter table fh_service_account add constraint fk_fh_service_account_fk_person_who_created foreign key (fk_person_who_created) references fh_person (id);

create index ix_fh_service_account_fk_portfolio_id on fh_service_account (fk_portfolio_id);
alter table fh_service_account add constraint fk_fh_service_account_fk_portfolio_id foreign key (fk_portfolio_id) references fh_portfolio (id);

create index ix_fh_service_account_env_fk_environment_id on fh_service_account_env (fk_environment_id);
alter table fh_service_account_env add constraint fk_fh_service_account_env_fk_environment_id foreign key (fk_environment_id) references fh_environment (id);

create index ix_fh_service_account_env_fk_service_account_id on fh_service_account_env (fk_service_account_id);
alter table fh_service_account_env add constraint fk_fh_service_account_env_fk_service_account_id foreign key (fk_service_account_id) references fh_service_account (id);

