-- apply changes
create table fh_app_feature_filter (
  fk_feature_id                 varchar2(40) not null,
  fk_filter_id                  varchar2(40) not null,
  constraint pk_fh_app_feature_filter primary key (fk_feature_id,fk_filter_id)
);

create table fh_feature_filter (
  id                            varchar2(40) not null,
  fk_portfolio_id               varchar2(40) not null,
  fk_person_who_created         varchar2(40),
  name                          varchar2(60) not null,
  description                   varchar2(300),
  when_updated                  timestamp not null,
  when_created                  timestamp not null,
  version                       number(19) not null,
  constraint idx_feature_filter_name unique (fk_portfolio_id,name),
  constraint pk_fh_feature_filter primary key (id)
);

create table fh_service_account_filter (
  fk_service_account_id         varchar2(40) not null,
  fk_filter_id                  varchar2(40) not null,
  constraint pk_fh_service_account_filter primary key (fk_service_account_id,fk_filter_id)
);

-- foreign keys and indices
create index ix_fh_pp_ftr_fltr_fh_pp_ftr on fh_app_feature_filter (fk_feature_id);
alter table fh_app_feature_filter add constraint fk_fh_pp_ftr_fltr_fh_pp_ftr foreign key (fk_feature_id) references fh_app_feature (id);

create index ix_fh_pp_ftr_fltr_fh_ftr_fltr on fh_app_feature_filter (fk_filter_id);
alter table fh_app_feature_filter add constraint fk_fh_pp_ftr_fltr_fh_ftr_fltr foreign key (fk_filter_id) references fh_feature_filter (id);

create index ix_fh_ftr_fltr_fk_prtfl_d on fh_feature_filter (fk_portfolio_id);
alter table fh_feature_filter add constraint fk_fh_ftr_fltr_fk_prtfl_d foreign key (fk_portfolio_id) references fh_portfolio (id);

create index ix_fh_ftr_fltr_fk_prsn_wh_crtd on fh_feature_filter (fk_person_who_created);
alter table fh_feature_filter add constraint fk_fh_ftr_fltr_fk_prsn_wh_crtd foreign key (fk_person_who_created) references fh_person (id);

create index ix_fh_srvc_ccnt_fltr_fh_3dsqys on fh_service_account_filter (fk_service_account_id);
alter table fh_service_account_filter add constraint fk_fh_srvc_ccnt_fltr_fh_hy6j72 foreign key (fk_service_account_id) references fh_service_account (id);

create index ix_fh_srvc_ccnt_fltr_fh_jdcl40 on fh_service_account_filter (fk_filter_id);
alter table fh_service_account_filter add constraint fk_fh_srvc_ccnt_fltr_fh_hjrzw6 foreign key (fk_filter_id) references fh_feature_filter (id);

