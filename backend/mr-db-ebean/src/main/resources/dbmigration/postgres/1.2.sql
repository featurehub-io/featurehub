-- apply changes
create table fh_userstate (
  id                            uuid not null,
  fk_person                     uuid not null,
  fk_portfolio_id               uuid,
  fk_app_id                     uuid,
  fk_env_id                     uuid,
  user_state                    varchar(15),
  data                          text,
  version                       bigint not null,
  when_updated                  timestamptz not null,
  when_created                  timestamptz not null,
  constraint ck_fh_userstate_user_state check ( user_state in ('HIDDEN_FEATURES')),
  constraint idx_user_state unique (fk_person,fk_portfolio_id,fk_app_id,fk_env_id),
  constraint pk_fh_userstate primary key (id)
);

create index ix_fh_userstate_fk_person on fh_userstate (fk_person);
alter table fh_userstate add constraint fk_fh_userstate_fk_person foreign key (fk_person) references fh_person (id) on delete restrict on update restrict;

create index ix_fh_userstate_fk_portfolio_id on fh_userstate (fk_portfolio_id);
alter table fh_userstate add constraint fk_fh_userstate_fk_portfolio_id foreign key (fk_portfolio_id) references fh_portfolio (id) on delete restrict on update restrict;

create index ix_fh_userstate_fk_app_id on fh_userstate (fk_app_id);
alter table fh_userstate add constraint fk_fh_userstate_fk_app_id foreign key (fk_app_id) references fh_application (id) on delete restrict on update restrict;

create index ix_fh_userstate_fk_env_id on fh_userstate (fk_env_id);
alter table fh_userstate add constraint fk_fh_userstate_fk_env_id foreign key (fk_env_id) references fh_environment (id) on delete restrict on update restrict;

