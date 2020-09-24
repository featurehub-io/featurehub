package io.featurehub.edge.strategies;

import io.featurehub.strategies.matchers.MatcherRepository;
import io.featurehub.sse.model.RolloutStrategy;
import io.featurehub.sse.model.RolloutStrategyAttribute;
import io.featurehub.sse.model.RolloutStrategyAttributeConditional;
import io.featurehub.sse.model.RolloutStrategyFieldType;
import io.featurehub.strategies.percentage.PercentageCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApplyFeature {
  private static final Logger log = LoggerFactory.getLogger(ApplyFeature.class);
  private final PercentageCalculator percentageCalculator;
  private final MatcherRepository matcherRepository;

  @Inject
  public ApplyFeature(PercentageCalculator percentageCalculator, MatcherRepository matcherRepository) {
    this.percentageCalculator = percentageCalculator;
    this.matcherRepository = matcherRepository;
  }

  public Applied applyFeature(List<RolloutStrategy> strategies, String key, String featureValueId,
                             ClientAttributeCollection cac) {
    if (cac != null & strategies != null && !strategies.isEmpty()) {
      Integer percentage = null;
      String percentageKey = null;
      Map<String, Integer> basePercentage = new HashMap<>();
      String defaultPercentageKey = cac.defaultPercentageKey();

      for(RolloutStrategy rsi : strategies ) {
        if (rsi.getPercentage() != null && (defaultPercentageKey != null || !rsi.getPercentageAttributes().isEmpty())) {
          // determine what the percentage key is
          String newPercentageKey = determinePercentageKey(cac, rsi.getPercentageAttributes());

          int basePercentageVal = basePercentage.computeIfAbsent(newPercentageKey, (k) -> 0);
          // if we have changed the key or we have never calculated it, calculate it and set the
          // base percentage to null
          if (percentage == null || !newPercentageKey.equals(percentageKey)) {
            percentageKey = newPercentageKey;

            percentage = percentageCalculator.determineClientPercentage(percentageKey,
              featureValueId);
            log.info("percentage for {} on {} calculated at {}", defaultPercentageKey, key, percentage);
          }

          log.info("comparing actual {} vs required: {}", percentage, rsi.getPercentage());
          int useBasePercentage = rsi.getAttributes() == null || rsi.getAttributes().isEmpty() ? basePercentageVal : 0;
            // if the percentage is lower than the user's key +
            // id of feature value then apply it
          if (percentage <= (useBasePercentage + rsi.getPercentage())) {
            if (rsi.getAttributes() != null && !rsi.getAttributes().isEmpty()) {
              if (matchAttributes(cac, rsi)) {
                return new Applied(true, rsi.getValue());
              }
            } else {
              return new Applied(true, rsi.getValue());
            }
          }

          // this was only a percentage and had no other attributes
          if (rsi.getAttributes() == null || rsi.getAttributes().isEmpty()) {
            basePercentage.put(percentageKey, basePercentage.get(percentageKey) + rsi.getPercentage());
          }
        } else if (rsi.getAttributes() != null && !rsi.getAttributes().isEmpty()) {
          if (matchAttributes(cac, rsi)) {
            return new Applied(true, rsi.getValue());
          }
        }
      }
    }

    return new Applied(false, null);
  }

  // This applies the rules as an AND. If at any point it fails it jumps out.
  private boolean matchAttributes(ClientAttributeCollection cac, RolloutStrategy rsi) {
    for(RolloutStrategyAttribute attr : rsi.getAttributes()) {
      String suppliedValue = cac.get(attr.getFieldName(), null);

      // "now" for dates and date-times are not passed by the client, so we create them in-situ
      if (suppliedValue == null && "now".equalsIgnoreCase(attr.getFieldName())) {
        if (attr.getType() == RolloutStrategyFieldType.DATE) {
          suppliedValue = DateTimeFormatter.ISO_DATE.format(LocalDateTime.now());
        } else if (attr.getType() == RolloutStrategyFieldType.DATETIME) {
          suppliedValue = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now());
        }
      }

      Object val = attr.getArray() ? attr.getValues() : attr.getValue();

      // both are null, just check against equals
      if (val == null && suppliedValue == null) {
        if (attr.getConditional() != RolloutStrategyAttributeConditional.EQUALS) {
          return false;
        }

        continue; // skip this one
      }

      // either of them are null, check against not equals as we can't do anything else
      if (val == null || suppliedValue == null) {
        if (attr.getConditional() != RolloutStrategyAttributeConditional.NOT_EQUALS) {
          return false;
        }

        continue; // can't compare with null and the only thing we can check is not equals
      }

      // find the appropriate matcher based on type and match against the supplied value
      if (!matcherRepository.findMatcher(suppliedValue, attr).match(suppliedValue, attr)) {
        return false;
      }
    }

    return true;
  }

  private String determinePercentageKey(ClientAttributeCollection cac, List<String> percentageAttributes) {
    if (percentageAttributes.isEmpty()) {
      return cac.defaultPercentageKey();
    }

    return percentageAttributes.stream().map(pa -> cac.get(pa, "<none>")).collect(Collectors.joining("$"));
  }


}
