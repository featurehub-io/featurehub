package io.featurehub.db.model;

import io.ebean.Model;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

@Embeddable
public class DbFeatureGroupFeatureKey extends Model {

  @Column(name = "fk_feat_id", nullable = false)
  @NotNull private UUID feature;
  @Column(name = "fk_fg_id", nullable = false)
  @NotNull private UUID group;

  public DbFeatureGroupFeatureKey(@NotNull UUID feature, @NotNull UUID featureGroup) {
    this.feature = feature;
    this.group = featureGroup;
  }

  public @NotNull UUID getGroup() {
    return group;
  }

  public @NotNull UUID getFeature() {
    return feature;
  }

  public void setFeature(@NotNull UUID feature) {
    this.feature = feature;
  }

  public void setGroup(@NotNull UUID group) {
    this.group = group;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DbFeatureGroupFeatureKey that = (DbFeatureGroupFeatureKey) o;

    if (!getFeature().equals(that.getFeature())) return false;
    return getGroup().equals(that.getGroup());
  }

  @Override
  public int hashCode() {
    int result = getFeature().hashCode();
    result = 31 * result + getGroup().hashCode();
    return result;
  }
}
