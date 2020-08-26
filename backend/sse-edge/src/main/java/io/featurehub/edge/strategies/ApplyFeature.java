package io.featurehub.edge.strategies;

import io.featurehub.edge.strategies.matchers.MatcherRepository;
import io.featurehub.edge.strategies.matchers.StrategyMatcher;
import io.featurehub.mr.model.RolloutStrategy;
import io.featurehub.mr.model.RolloutStrategyAttribute;
import io.featurehub.mr.model.RolloutStrategyAttributeConditional;
import io.featurehub.mr.model.RolloutStrategyFieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
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
      String userKey = cac.userKey();

      for(RolloutStrategy rsi : strategies ) {
        if (rsi.getPercentage() != null && (userKey != null || !rsi.getPercentageAttributes().isEmpty())) {
          // determine what the percentage key is
          String newPercentageKey = determinePercentageKey(cac, rsi.getPercentageAttributes());

          int basePercentageVal = basePercentage.computeIfAbsent(newPercentageKey, (k) -> 0);
          // if we have changed the key or we have never calculated it, calculate it and set the
          // base percentage to null
          if (percentage == null || !newPercentageKey.equals(percentageKey)) {
            percentageKey = newPercentageKey;

            percentage = percentageCalculator.determineClientPercentage(percentageKey,
              featureValueId);
            log.info("percentage for {} on {} calculated at {}", cac.userKey(), key, percentage);
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
        return (attr.getConditional() == RolloutStrategyAttributeConditional.EQUALS);
      }

      // either of them are null, check against not equals as we can't do anything else
      if (val == null || suppliedValue == null) {
        return (attr.getConditional() == RolloutStrategyAttributeConditional.NOT_EQUALS);
      }

      // find the appropriate matcher based on type and match against the supplied value
      return matcherRepository.findMatcher(suppliedValue, attr).match(suppliedValue, attr);
    }

    return false;
  }

  private String determinePercentageKey(ClientAttributeCollection cac, List<String> percentageAttributes) {
    if (percentageAttributes.isEmpty()) {
      return cac.userKey();
    }

    return percentageAttributes.stream().map(pa -> cac.get(pa, "<none>")).collect(Collectors.joining("$"));
  }


}
