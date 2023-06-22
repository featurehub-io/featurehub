package io.featurehub.db.model;

import io.ebean.annotation.DbJson;
import io.featurehub.db.model.query.QDbStrategyForFeatureValue;
import io.featurehub.mr.model.RolloutStrategy;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "fh_fv_version")
public class DbFeatureValueVersion extends DbBaseFeatureValue {
  @EmbeddedId
  private final DbFeatureValueVersionKey id;

  public DbFeatureValueVersion(@NotNull DbFeatureValueVersionKey id,
                               @NotNull LocalDateTime whenCreated,
                               @NotNull DbPerson whoCreated,
                               @Nullable String defaultValue,
                               boolean locked, boolean retired,
                               @NotNull List<RolloutStrategy> rolloutStrategies,
                               @NotNull List<SharedRolloutStrategyVersion> sharedRolloutStrategies,
                               DbApplicationFeature feature,
                               @Nullable Long versionFrom) {
    super(whoCreated, locked);

    this.id = id;
    this.whenCreated = whenCreated;

    this.retired = retired;
    this.sharedRolloutStrategies = sharedRolloutStrategies;
    this.feature = feature;
    this.versionFrom = versionFrom;

    setDefaultValue(defaultValue);
    setRolloutStrategies(rolloutStrategies);
  }

  @ManyToOne(optional = false)
  @Column(name = "id")
  @JoinColumn(name="id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
  @MapsId("id")
  private DbFeatureValue featureValue;

  @NotNull public DbFeatureValue getFeatureValue() {
    return featureValue;
  }

  private boolean retired;

  @ManyToOne(optional = false)
  private DbApplicationFeature feature;

  /**
   * The version the user was updating when they saved this record. Allows us to look  up  that version, do
   *  a diff between these records and send the details of the diff back.
   */
  @Column(name = "v_from")
  @Nullable
  private Long versionFrom;

  @DbJson
  @Column(name = "shared_strat")
  protected List<SharedRolloutStrategyVersion> sharedRolloutStrategies;

  public @NotNull List<SharedRolloutStrategyVersion> getSharedRolloutStrategies() {
    if (sharedRolloutStrategies == null) {
      sharedRolloutStrategies = new LinkedList<>();
    }

    return sharedRolloutStrategies;
  }

  public boolean isRetired() {
    return retired;
  }

  public DbApplicationFeature getFeature() {
    return feature;
  }

  public DbFeatureValueVersionKey getId() {
    return id;
  }

  public static DbFeatureValueVersion fromDbFeatureValue(DbFeatureValue from, @Nullable Long versionFrom) {
    return new DbFeatureValueVersion(
      new DbFeatureValueVersionKey(from.getId(), from.getVersion()),
        from.getVersion() == 1L ? from.getWhenCreated() : from.getWhenUpdated(),
        from.getWhoUpdated(),
        from.getDefaultValue(),
        from.isLocked(),
        from.getRetired() == Boolean.TRUE,
        from.getRolloutStrategies(),
        transformSharedStrategies(from.getSharedRolloutStrategies()),
        from.getFeature(),
        versionFrom
      );
  }

  private static List<SharedRolloutStrategyVersion> transformSharedStrategies(@NotNull List<DbStrategyForFeatureValue> sharedRolloutStrategies) {
    return new QDbStrategyForFeatureValue()
      .id.in(sharedRolloutStrategies.stream().map(DbStrategyForFeatureValue::getId).collect(Collectors.toList()))
      .select(QDbStrategyForFeatureValue.Alias.rolloutStrategy.id,
        QDbStrategyForFeatureValue.Alias.rolloutStrategy.version, QDbStrategyForFeatureValue.Alias.value,
        QDbStrategyForFeatureValue.Alias.enabled).findStream().map(shared -> {
          return new SharedRolloutStrategyVersion(shared.getRolloutStrategy().getId(),
            shared.getRolloutStrategy().getVersion(),
            shared.isEnabled(), shared.getValue());
      }).collect(Collectors.toList());
  }

  public @Nullable Long getVersionFrom() {
    return versionFrom;
  }
}
