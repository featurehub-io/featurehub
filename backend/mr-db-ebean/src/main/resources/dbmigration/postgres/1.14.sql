-- ignore this migration
-- drop dependencies
-- alter table if exists fh_person_group_link drop constraint if exists fk_fh_person_group_link_fh_person;
-- alter table if exists fh_person_group_link drop constraint if exists fk_fh_person_group_link_fh_group;
-- foreign keys and indices
-- alter table fh_person_group_link add constraint fk_fh_person_group_link_fk_person_id foreign key (fk_person_id) references fh_person (id) on delete restrict on update restrict;alter table fh_person_group_link add constraint fk_fh_person_group_link_fh_person foreign key (fk_person_id) references fh_person (id) on delete restrict on update restrict;
-- alter table fh_person_group_link add constraint fk_fh_person_group_link_fk_group_id foreign key (fk_group_id) references fh_group (id) on delete restrict on update restrict;
