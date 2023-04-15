package io.featurehub.db.model;

import io.ebean.annotation.Aggregation;
import io.ebean.annotation.View;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.util.UUID;

@Entity
@View(name = "fh_person_group_link", dependentTables = "fh_person_group_link")
public class GroupMemberAgg {
  @Aggregation("count(fk_group_id)")
  public Integer counter;

  @Column(name="fk_person_id")
  public UUID personId;

}
