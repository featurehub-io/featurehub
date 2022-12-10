package io.featurehub.db.model;

import io.ebean.annotation.DbJson;
import io.featurehub.db.model.query.QDbStrategyForFeatureValue;
import io.featurehub.mr.model.RolloutStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "fh_fv_version")
public class DbFeatureValueVersion extends DbBaseFeatureValue {
  @EmbeddedId
  private final DbFeatureValueVersionKey id;

  public DbFeatureValueVersion(@NotNull DbFeatureValueVersionKey id, @NotNull LocalDateTime whenCreated,
                               DbPerson whoCreated, @NotNull FeatureState featureState,
                               @Nullable String defaultValue, boolean locked, boolean retired,
                               List<RolloutStrategy> rolloutStrategies,
                               List<SharedRolloutStrategyVersion> sharedRolloutStrategies,
                               DbApplicationFeature feature) {

    this.id = id;
    this.whenCreated = whenCreated;

    this.retired = retired;
    this.sharedRolloutStrategies = sharedRolloutStrategies;
    this.feature = feature;

    setWhoUpdated(whoCreated);
    setFeatureState(featureState);
    setDefaultValue(defaultValue);
    setLocked(locked);
    setRolloutStrategies(rolloutStrategies);
  }

  private boolean retired;

  @ManyToOne(optional = false)
  private DbApplicationFeature feature;

  @DbJson
  @Column(name = "shared_strat")
  protected List<SharedRolloutStrategyVersion> sharedRolloutStrategies;

  public List<SharedRolloutStrategyVersion> getSharedRolloutStrategies() {
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

  public static DbFeatureValueVersion fromDbFeatureValue(DbFeatureValue from) {
    return new DbFeatureValueVersion(
      new DbFeatureValueVersionKey(from.getId(), from.getVersion()),
        from.getVersion() == 1L ? from.getWhenCreated() : from.getWhenUpdated(),
        from.getWhoUpdated(),
        from.getFeatureState() == null ? FeatureState.READY : from.getFeatureState(),
        from.getDefaultValue(),
        from.isLocked(),
        from.getRetired() == Boolean.TRUE,
        from.getRolloutStrategies() == null ? Collections.emptyList() : from.getRolloutStrategies(),
        transformSharedStrategies(from.getSharedRolloutStrategies()),
        from.getFeature()
      );
  }

  private static List<SharedRolloutStrategyVersion> transformSharedStrategies(List<DbStrategyForFeatureValue> sharedRolloutStrategies) {
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
}
