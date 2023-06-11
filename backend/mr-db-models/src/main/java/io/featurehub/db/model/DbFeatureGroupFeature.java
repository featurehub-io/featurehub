package io.featurehub.db.model;

import io.ebean.Model;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "fh_fg_feat")
public class DbFeatureGroupFeature extends Model {
  @EmbeddedId
  @NotNull private DbFeatureGroupFeatureKey key;

  @Nullable
  @Lob
  @Column(name = "v")
  private String value;

  @ManyToOne(optional = false)
  @MapsId("feature")
  @Column(name = "fk_feat_id", nullable = false)
  @JoinColumn(name = "fk_feat_id", referencedColumnName = "id", updatable = false, nullable = false)
  private DbApplicationFeature feature;

  @ManyToOne(optional = false)
  @MapsId("group")
  @Column(name = "fk_fg_id", nullable = false)
  @JoinColumn(name = "fk_fg_id", referencedColumnName = "id", updatable = false, nullable = false)
  private DbFeatureGroup group;

  public DbFeatureGroupFeature(@NotNull DbFeatureGroupFeatureKey key) {
    this.key = key;
  }

  public @NotNull DbFeatureGroupFeatureKey getKey() {
    return key;
  }

  public @Nullable String getValue() {
    return value;
  }

  public void setValue(@Nullable String value) {
    this.value = value;
  }

  @NotNull public DbApplicationFeature getFeature() {
    return feature;
  }

  @NotNull public DbFeatureGroup getGroup() {
    return group;
  }

  public void setFeature(DbApplicationFeature feature) {
    this.feature = feature;
  }

  public void setGroup(DbFeatureGroup group) {
    this.group = group;
  }
}
