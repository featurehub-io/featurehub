package io.featurehub.db.model;

import io.ebean.annotation.DbJson;
import io.featurehub.db.model.query.QDbStrategyForFeatureValue;
import io.featurehub.mr.model.RolloutStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "fh_fv_version")
public class DbFeatureValueVersion extends DbBaseFeatureValue {
  @EmbeddedId
  private final DbFeatureValueVersionKey id;

  public DbFeatureValueVersion(@NotNull DbFeatureValueVersionKey id, @NotNull LocalDateTime whenCreated,
                               @NotNull DbPerson whoCreated, @NotNull FeatureState featureState,
                               @Nullable String defaultValue, boolean locked, boolean retired,
                               List<RolloutStrategy> rolloutStrategies,
                               List<SharedRolloutStrategyVersion> sharedRolloutStrategies ) {

    this.id = id;
    this.whenCreated = whenCreated;
    setWhoUpdated(whoCreated);
    setFeatureState(featureState);
    setDefaultValue(defaultValue);
    setLocked(locked);
    setRetired(retired);
    setRolloutStrategies(rolloutStrategies);
    setSharedRolloutStrategies(sharedRolloutStrategies);
  }

  boolean retired;

  @DbJson
  @Column(name = "shared_strat")
  protected List<SharedRolloutStrategyVersion> sharedRolloutStrategies;

  public List<SharedRolloutStrategyVersion> getSharedRolloutStrategies() {
    return sharedRolloutStrategies;
  }

  public void setSharedRolloutStrategies(List<SharedRolloutStrategyVersion> sharedRolloutStrategies) {
    this.sharedRolloutStrategies = sharedRolloutStrategies;
  }

  public boolean isRetired() {
    return retired;
  }

  public void setRetired(boolean retired) {
    this.retired = retired;
  }

  public static DbFeatureValueVersion fromDbFeatureValue(DbFeatureValue from) {
    return new DbFeatureValueVersion(
      new DbFeatureValueVersionKey(from.getId(), from.getVersion()),
        from.getVersion() == 1L ? from.getWhenCreated() : from.getWhenUpdated(),
        from.getWhoUpdated(),
        from.getFeatureState(),
        from.getDefaultValue(),
        from.isLocked(),
        from.getRetired() == Boolean.TRUE,
        from.getRolloutStrategies(),
        transformSharedStrategies(from.getSharedRolloutStrategies())
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
