package io.featurehub.db.model;

import io.ebean.annotation.Max;
import io.ebean.annotation.View;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@View(name = "fh_featgroup", dependentTables = "fh_featgroup")
public class FeatureGroupOrderHighest {
  @Max
  @Column(name = "gp_order")
  public Integer highest;

  @Column(name = "fk_environment_id", nullable = false)
  public UUID environmentId;
}
