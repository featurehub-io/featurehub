-- apply changes
alter table fh_env_feature_strategy add constraint idx_fv_unique unique  (fk_environment_id,fk_feature_id);
