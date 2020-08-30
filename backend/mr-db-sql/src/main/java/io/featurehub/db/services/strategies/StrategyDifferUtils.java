package io.featurehub.db.services.strategies;

import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbFeatureValue;
import io.featurehub.db.model.DbRolloutStrategy;
import io.featurehub.db.model.DbStrategyForFeatureValue;
import io.featurehub.db.model.query.QDbApplication;
import io.featurehub.db.model.query.QDbRolloutStrategy;
import io.featurehub.db.services.Conversions;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.FeatureValueType;
import io.featurehub.mr.model.RolloutStrategyInstance;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StrategyDifferUtils implements StrategyDiffer {
  @Override
  public boolean invalidStrategyInstances(List<RolloutStrategyInstance> instances, DbApplicationFeature feature) {
    if (instances != null) {
      if (feature.getValueType() == FeatureValueType.BOOLEAN) {
        return instances.stream().anyMatch(i -> i.getValue() == null);
      }
    }

    return false;
  }

  @Override
  public ChangedSharedStrategies createDiff(FeatureValue featureValue, DbFeatureValue strategy) {
    ChangedSharedStrategies css = new ChangedSharedStrategies();

    if (!featureValue.getRolloutStrategyInstances().isEmpty() || !strategy.getSharedRolloutStrategies().isEmpty()) {
      // lets first make sure the shared strategies are in our application.
      Map<DbRolloutStrategy, RolloutStrategyInstance> sharedStrats =
        featureValue.getRolloutStrategyInstances().stream().map(rsi -> {
          UUID sId = Conversions.ifUuid(rsi.getStrategyId());
          if (sId != null) {
            final DbRolloutStrategy rs =
              new QDbRolloutStrategy().id.eq(sId).application.fetch(QDbApplication.alias().id).findOne();

            if (rs != null && rs.getApplication().getId().equals(strategy.getEnvironment().getParentApplication().getId())) {
              return Map.entry(rs, rsi);
            }
          }

          return null;
        }).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      if (!sharedStrats.isEmpty()) {
        // rollout strategy id, intermediate table
        Map<UUID, DbStrategyForFeatureValue> existing =
          strategy.getSharedRolloutStrategies().stream().collect(Collectors.toMap(s -> s.getRolloutStrategy().getId(),
            Function.identity() ));

        // these are the incoming ones. They can consist of new ones, updating existing ones, and the ones that
        // are missing from the existing list should become deletes
        sharedStrats.forEach((dbStrat, rsi) -> {
          // this rollout strategy has already got a mapping?
          DbStrategyForFeatureValue found = existing.get(dbStrat.getId());

          if (found != null) {
            // if the value changed or the disabled changed
            if ((rsi.getValue() == null && found.getValue() != null) ||
              (rsi.getValue() != null && found.getValue() == null) ||
              found.getValue().equals(rsi.getValue().toString()) ||
              ((!Boolean.FALSE.equals(rsi.getDisabled()) == found.isEnabled()))
            ) {
              found.setValue(rsi.getValue() == null ? null : rsi.getValue().toString());
              found.setEnabled(!Boolean.FALSE.equals(rsi.getDisabled()));
              css.updatedStrategies.add(found);
              existing.remove(dbStrat.getId()); // remove it so the left over oens are deleted
            }
          } else {
            css.updatedStrategies.add(new DbStrategyForFeatureValue.Builder()
              .featureValue(strategy)
              .enabled(!Boolean.FALSE.equals(rsi.getDisabled()))
              .rolloutStrategy(dbStrat)
              .value(rsi.getValue() == null ? null : rsi.getValue().toString())
              .build()
              );
          }
        });

        css.deletedStrategies.addAll(existing.values());
      }
    }

    return css.isEmpty() ? null : css;
  }
}
