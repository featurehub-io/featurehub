-- apply changes
create table fh_userstate (
  id                            uniqueidentifier not null,
  fk_person                     uniqueidentifier not null,
  fk_portfolio_id               uniqueidentifier,
  fk_app_id                     uniqueidentifier,
  fk_env_id                     uniqueidentifier,
  user_state                    nvarchar(15),
  data                          nvarchar(max),
  version                       numeric(19) not null,
  when_updated                  datetime2 not null,
  when_created                  datetime2 not null,
  constraint ck_fh_userstate_user_state check ( user_state in ('HIDDEN_FEATURES')),
  constraint pk_fh_userstate primary key (id)
);
create unique nonclustered index idx_user_state on fh_userstate(fk_person,fk_portfolio_id,fk_app_id,fk_env_id) where fk_portfolio_id is not null and fk_app_id is not null and fk_env_id is not null;

create index ix_fh_userstate_fk_person on fh_userstate (fk_person);
alter table fh_userstate add constraint fk_fh_userstate_fk_person foreign key (fk_person) references fh_person (id);

create index ix_fh_userstate_fk_portfolio_id on fh_userstate (fk_portfolio_id);
alter table fh_userstate add constraint fk_fh_userstate_fk_portfolio_id foreign key (fk_portfolio_id) references fh_portfolio (id);

create index ix_fh_userstate_fk_app_id on fh_userstate (fk_app_id);
alter table fh_userstate add constraint fk_fh_userstate_fk_app_id foreign key (fk_app_id) references fh_application (id);

create index ix_fh_userstate_fk_env_id on fh_userstate (fk_env_id);
alter table fh_userstate add constraint fk_fh_userstate_fk_env_id foreign key (fk_env_id) references fh_environment (id);

