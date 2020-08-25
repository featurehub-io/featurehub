package io.featurehub.edge.strategies;

import io.featurehub.mr.model.RolloutStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class ApplyFeature {
  private static final Logger log = LoggerFactory.getLogger(ApplyFeature.class);
  private final PercentageCalculator percentageCalculator;

  @Inject
  public ApplyFeature(PercentageCalculator percentageCalculator) {
    this.percentageCalculator = percentageCalculator;
  }

  public Applied applyFeature(List<RolloutStrategy> strategies, String key, String featureValueId,
                             ClientAttributeCollection cac) {
    if (cac != null & strategies != null && !strategies.isEmpty()) {
      Integer percentage = null;
      int basePercentage = 0;
      String userKey = cac.userKey();
      for(RolloutStrategy rsi : strategies ) {
        if (rsi.getPercentage() != null && (userKey != null || !rsi.getPercentageAttributes().isEmpty())) {
          if (percentage == null) {
            percentage = percentageCalculator.determineClientPercentage(determinePercentageKey(cac,
              rsi.getPercentageAttributes()),
              featureValueId);
            log.info("percentage for {} on {} calculated at {}", cac.userKey(), key, percentage);
          }

          log.info("comparing actual {} vs required: {}", percentage, rsi.getPercentage());
          int useBasePercentage = rsi.getAttributes() == null || rsi.getAttributes().isEmpty() ? basePercentage : 0;
            // if the percentage is lower than the user's key +
            // id of feature value then apply it
          if (percentage <= (useBasePercentage + rsi.getPercentage())) {
            return new Applied(true, rsi.getValue());
          }

          // this was only a percentage and had no other attributes
          if (rsi.getAttributes() == null || rsi.getAttributes().isEmpty()) {
            basePercentage += rsi.getPercentage();
          }
        }
      }
    }

    return new Applied(false, null);
  }

  private String determinePercentageKey(ClientAttributeCollection cac, List<String> percentageAttributes) {
    if (percentageAttributes.isEmpty()) {
      return cac.userKey();
    }

    return percentageAttributes.stream().map(pa -> cac.get(pa, "<none>")).collect(Collectors.joining("$"));
  }


}
