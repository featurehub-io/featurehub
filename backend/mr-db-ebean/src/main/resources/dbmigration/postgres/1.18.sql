-- apply changes
create table fh_webhook (
  id                            uuid not null,
  environment_id                uuid not null,
  when_sent                     timestamptz not null,
  status                        integer not null,
  method                        varchar(7) not null,
  ce_type                       varchar(100) not null,
  whce_type                     varchar(100) not null,
  json                          text not null,
  constraint pk_fh_webhook primary key (id)
);

-- foreign keys and indices
create index ix_fh_webhook_environment_id on fh_webhook (environment_id);
alter table fh_webhook add constraint fk_fh_webhook_environment_id foreign key (environment_id) references fh_environment (id) on delete restrict on update restrict;

create index if not exists idx_webhook on fh_webhook (environment_id,when_sent);
create index if not exists idx_webhook_type on fh_webhook (environment_id,when_sent,whce_type);
