package io.featurehub.edge.strategies;

import io.featurehub.mr.model.RolloutStrategyAttributeConditional;
import io.featurehub.mr.model.RolloutStrategyFieldType;
import io.featurehub.sse.model.FeatureRolloutStrategy;
import io.featurehub.sse.model.FeatureRolloutStrategyAttribute;
import io.featurehub.strategies.matchers.MatcherRepository;
import io.featurehub.strategies.matchers.WhenActiveMatcher;
import io.featurehub.strategies.percentage.PercentageCalculator;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApplyFeature {
  private static final Logger log = LoggerFactory.getLogger(ApplyFeature.class);
  private final PercentageCalculator percentageCalculator;
  private final MatcherRepository matcherRepository;
  private final WhenActiveMatcher whenActiveMatcher;
  private final Clock clock;

  @Inject
  public ApplyFeature(PercentageCalculator percentageCalculator, MatcherRepository matcherRepository) {
    this(percentageCalculator, matcherRepository, Clock.systemUTC());
  }

  /**
   * @param clock the clock used for "now" and fhubWhenActive, so tests can fix the time
   */
  public ApplyFeature(PercentageCalculator percentageCalculator, MatcherRepository matcherRepository, Clock clock) {
    this.percentageCalculator = percentageCalculator;
    this.matcherRepository = matcherRepository;
    this.clock = clock;
    this.whenActiveMatcher = new WhenActiveMatcher(clock);
  }

  public Applied applyFeature(List<FeatureRolloutStrategy> strategies, String key, String featureValueId,
                              ClientContext cac) {
    return applyFeature(strategies, key, featureValueId, cac, clock.instant());
  }

  /**
   * @param now the time to evaluate at: anything clock based (fhubWhenActive, "now") uses this one instant, so a
   *            request's etag and its evaluated values can be worked out from the same moment
   */
  public Applied applyFeature(List<FeatureRolloutStrategy> strategies, String key, String featureValueId,
                              ClientContext cac, Instant now) {
    if (cac != null & strategies != null && !strategies.isEmpty()) {
      Integer percentage = null;
      String percentageKey = null;
      Map<String, Integer> basePercentage = new HashMap<>();
      String defaultPercentageKey = cac.defaultPercentageKey();

      for(FeatureRolloutStrategy rsi : strategies ) {
        if (rsi.getPercentage() != null && (defaultPercentageKey != null || (rsi.getPercentageAttributes() != null && !rsi.getPercentageAttributes().isEmpty()))) {
          // determine what the percentage key is
          String newPercentageKey = determinePercentageKey(cac, rsi.getPercentageAttributes());

          int basePercentageVal = basePercentage.computeIfAbsent(newPercentageKey, (k) -> 0);
          // if we have changed the key or we have never calculated it, calculate it and set the
          // base percentage to null
          if (percentage == null || !newPercentageKey.equals(percentageKey)) {
            percentageKey = newPercentageKey;

            percentage = percentageCalculator.determineClientPercentage(percentageKey,
              featureValueId);
            log.trace("percentage for {} on {} calculated at {}", defaultPercentageKey, key, percentage);
          }

          log.trace("comparing actual {} vs required: {}", percentage, rsi.getPercentage());
          int useBasePercentage = rsi.getAttributes().isEmpty() ? basePercentageVal : 0;
            // if the percentage is lower than the user's key +
            // id of feature value then apply it
          if (percentage <= (useBasePercentage + rsi.getPercentage())) {
            if (!rsi.getAttributes().isEmpty()) {
              if (matchAttributes(cac, rsi, now)) {
                return new Applied(true, rsi.getId(), rsi.getValue());
              }
            } else {
              return new Applied(true, rsi.getId(), rsi.getValue());
            }
          }

          // this was only a percentage and had no other attributes
          if (rsi.getAttributes().isEmpty()) {
            basePercentage.put(percentageKey, basePercentage.get(percentageKey) + rsi.getPercentage());
          }
        }

        if ((rsi.getPercentage() == null || rsi.getPercentage() == 0) &&
          !rsi.getAttributes().isEmpty()) {
          if (matchAttributes(cac, rsi, now)) {
            return new Applied(true, rsi.getId(), rsi.getValue());
          }
        }
      }
    }

    return Applied.noMatch();
  }

  // This applies the rules as an AND. If at any point it fails it jumps out.
  private boolean matchAttributes(ClientContext cac, FeatureRolloutStrategy rsi, Instant now) {
    for(FeatureRolloutStrategyAttribute attr : rsi.getAttributes()) {
      // fhubWhenActive compares Edge's clock against the attribute's date-time, ignoring its type
      // and never looking at the client context, so the client cannot supply or override it
      if (WhenActiveMatcher.isWhenActive(attr.getFieldName())) {
        if (!whenActiveMatcher.match(attr, now)) {
          return false;
        }

        continue;
      }

      List<String> suppliedValues = cac.get(attr.getFieldName());

      // "now" for dates and date-times are not passed by the client, so we create them in-situ, in UTC
      if ((suppliedValues == null || suppliedValues.isEmpty()) && "now".equalsIgnoreCase(attr.getFieldName())) {
        if (attr.getType() == RolloutStrategyFieldType.DATE) {
          suppliedValues = List.of(DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.ofInstant(now, ZoneOffset.UTC)));
        } else if (attr.getType() == RolloutStrategyFieldType.DATETIME) {
          suppliedValues = List.of(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.ofInstant(now, ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS)));
        }
      }

      Object val = attr.getValues();

      // both are null, just check against equals
      if (val == null && suppliedValues == null) {
        if (attr.getConditional() != RolloutStrategyAttributeConditional.EQUALS) {
          return false;
        }

        continue; // skip this one
      }

      // either of them are null, check against not equals as we can't do anything else
      if (val == null || suppliedValues == null) {
        return false;
      }

      // if none of the supplied values match against the associated matcher,
      if (suppliedValues.stream().noneMatch(sv -> matcherRepository.findMatcher(attr).match(sv, attr) )) {
        return false;
      }
    }

    return true;
  }

  private String determinePercentageKey(ClientContext cac, List<String> percentageAttributes) {
    if (percentageAttributes == null || percentageAttributes.isEmpty()) {
      return cac.defaultPercentageKey();
    }

    return percentageAttributes.stream()
      .map(pa -> cac.get(pa, "<none>"))
      .flatMap(Collection::stream).collect(Collectors.joining(
      "$"));
  }


}
