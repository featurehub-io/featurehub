-- apply changes
create table fh_app_feature_filter (
  fk_feature_id                 varchar(40) not null,
  fk_filter_id                  varchar(40) not null,
  constraint pk_fh_app_feature_filter primary key (fk_feature_id,fk_filter_id)
);

create table fh_feature_filter (
  id                            varchar(40) not null,
  fk_portfolio_id               varchar(40) not null,
  fk_person_who_created         varchar(40),
  name                          varchar(60) not null,
  description                   varchar(300),
  when_updated                  datetime(6) not null,
  when_created                  datetime(6) not null,
  version                       bigint not null,
  constraint idx_feature_filter_name unique (fk_portfolio_id,name),
  constraint pk_fh_feature_filter primary key (id)
);

create table fh_service_account_filter (
  fk_service_account_id         varchar(40) not null,
  fk_filter_id                  varchar(40) not null,
  constraint pk_fh_service_account_filter primary key (fk_service_account_id,fk_filter_id)
);

-- foreign keys and indices
create index ix_fh_app_feature_filter_fh_app_feature on fh_app_feature_filter (fk_feature_id);
alter table fh_app_feature_filter add constraint fk_fh_app_feature_filter_fh_app_feature foreign key (fk_feature_id) references fh_app_feature (id) on delete restrict on update restrict;

create index ix_fh_app_feature_filter_fh_feature_filter on fh_app_feature_filter (fk_filter_id);
alter table fh_app_feature_filter add constraint fk_fh_app_feature_filter_fh_feature_filter foreign key (fk_filter_id) references fh_feature_filter (id) on delete restrict on update restrict;

create index ix_fh_feature_filter_fk_portfolio_id on fh_feature_filter (fk_portfolio_id);
alter table fh_feature_filter add constraint fk_fh_feature_filter_fk_portfolio_id foreign key (fk_portfolio_id) references fh_portfolio (id) on delete restrict on update restrict;

create index ix_fh_feature_filter_fk_person_who_created on fh_feature_filter (fk_person_who_created);
alter table fh_feature_filter add constraint fk_fh_feature_filter_fk_person_who_created foreign key (fk_person_who_created) references fh_person (id) on delete restrict on update restrict;

create index ix_fh_service_account_filter_fh_service_account on fh_service_account_filter (fk_service_account_id);
alter table fh_service_account_filter add constraint fk_fh_service_account_filter_fh_service_account foreign key (fk_service_account_id) references fh_service_account (id) on delete restrict on update restrict;

create index ix_fh_service_account_filter_fh_feature_filter on fh_service_account_filter (fk_filter_id);
alter table fh_service_account_filter add constraint fk_fh_service_account_filter_fh_feature_filter foreign key (fk_filter_id) references fh_feature_filter (id) on delete restrict on update restrict;

