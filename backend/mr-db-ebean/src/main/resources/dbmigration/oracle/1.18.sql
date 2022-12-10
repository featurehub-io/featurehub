-- apply changes
create table fh_webhook (
  id                            varchar2(40) not null,
  environment_id                varchar2(40) not null,
  when_sent                     timestamp not null,
  method                        varchar2(7) not null,
  status                        number(10) not null,
  ce_type                       varchar2(100) not null,
  whce_type                     varchar2(100) not null,
  json                          clob not null,
  constraint pk_fh_webhook primary key (id)
);

-- foreign keys and indices
create index ix_fh_webhook_environment_id on fh_webhook (environment_id);
alter table fh_webhook add constraint fk_fh_webhook_environment_id foreign key (environment_id) references fh_environment (id);

create index idx_webhook on fh_webhook (environment_id,when_sent);
create index idx_webhook_type on fh_webhook (environment_id,when_sent,whce_type);
