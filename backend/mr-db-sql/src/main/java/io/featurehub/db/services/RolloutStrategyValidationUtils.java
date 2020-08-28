package io.featurehub.db.services;

import io.featurehub.db.api.RolloutStrategyValidator;
import io.featurehub.mr.model.RolloutStrategy;
import io.featurehub.mr.model.RolloutStrategyAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RolloutStrategyValidationUtils implements RolloutStrategyValidator {
  private static final Logger log = LoggerFactory.getLogger(RolloutStrategyValidationUtils.class);
  public static final int MAX_PERCENTAGE = 1000000; // four decimal points

  public void validateStrategies(List<RolloutStrategy> strategies)
    throws PercentageStrategyGreaterThan100Percent, InvalidStrategyCombination {

    if (strategies == null || strategies.isEmpty()) {
      return;
    }

    // if any of them have no attributes and no percentage (or invalid percentage) then this is invalid
    // or no name or a name that is > 200 long
    if (strategies.stream().anyMatch(rsi -> rsi.getName() == null || rsi.getName().isEmpty() || rsi.getName().trim().length() > 200)) {
      log.warn("feature rollout strategy contained at least one invalid name {}", strategies);
      throw new InvalidStrategyCombination();
    }


    if (strategies.stream().anyMatch(rsi -> (
      rsi.getAttributes().isEmpty() && (rsi.getPercentage() == null || rsi.getPercentage() < 0)))) {
      log.warn("Found empty strategy or strategy with negative %: {}", strategies);
      throw new InvalidStrategyCombination();
    }

    // if any percentage is > max percentage then fail.
    if (strategies.stream().anyMatch(rs -> rs.getPercentage() != null && rs.getPercentage() > MAX_PERCENTAGE)) {
      log.warn("At least one strategy has a percentage of > 100% {}", strategies);
      throw new PercentageStrategyGreaterThan100Percent();
    }

    // if total amount of > MAX_PERCENTAGE for only percentage strategies not allowed
    if (strategies.stream()
      .filter(rsi -> rsi.getAttributes() == null || rsi.getAttributes().isEmpty())
      .map(RolloutStrategy::getPercentage).reduce(0,
        Integer::sum) > MAX_PERCENTAGE) {
      log.warn("Percentage adds up to > 1000000 which is the baseline for percentage rollout {}.", strategies);
      throw new PercentageStrategyGreaterThan100Percent();
    }

    // if array type and no values in array
    if (strategies.stream().anyMatch(rs ->
      rs.getAttributes()
        .stream()
        .anyMatch(a -> Boolean.TRUE.equals(a.getArray()) && (a.getValues() == null || a.getValues().isEmpty())))) {
      log.warn("At least one strategy indicates an array and array is empty");
      throw new InvalidStrategyCombination();
    }

    for (RolloutStrategy rs : strategies) {
      if (!rs.getAttributes().isEmpty()) {
        for (RolloutStrategyAttribute attr : rs.getAttributes()) {
          if (attr.getConditional() == null) {
            log.warn("strategy {} invalid because missing conditional", rs);
            throw new InvalidStrategyCombination();
          }

          if (attr.getFieldName() == null) {
            log.warn("strategy {} invalid because missing fieldName", rs);
            throw new InvalidStrategyCombination();
          }

          if (attr.getType() == null) {
            log.warn("strategy {} invalid because missing field type", rs);
            throw new InvalidStrategyCombination();
          }

          if (Boolean.TRUE.equals(attr.getArray())) {
            attr.setValue(null);
            try {
              switch (attr.getType()) {
                case STRING:
                  attr.setValues(attr.getValues().stream().map(v -> v.toString().trim()).collect(Collectors.toList()));
                  break;
                case SEMANTIC_VERSION:
                  break;
                case NUMBER:
                  if (attr.getValues().stream().anyMatch(n -> n == null || notNumber(n))) {
                    log.warn("one of strategy values is not a number: {}", attr);
                    throw new InvalidStrategyCombination();
                  }
                  break;
                case DATE:
                  if (attr.getValues().stream().anyMatch(n -> n == null || notDate(n))) {
                    log.warn("one of strategy values is not a date: {}", attr);
                  }
                  break;
                case DATETIME:
                  if (attr.getValues().stream().anyMatch(n -> n == null || notDateTime(n))) {
                    log.warn("one of strategy values is not a date: {}", attr);
                  }
                  break;
                case BOOLEAN:
                  attr.setValues(attr.getValues().stream().filter(Objects::nonNull)
                    .map(b -> Boolean.parseBoolean(b.toString())).collect(Collectors.toList()));
                  break;
                case IP_ADDRESS:
                  break;
              }
            } catch (Exception e) {
              log.warn("Failed to cleanup strategy {}", rs, e);
              throw new InvalidStrategyCombination();
            }
          } else {
            attr.getValues().clear();
          }
        }
      }
    }
  }



  private boolean notDate(Object n) {
    try {
      LocalDate.from(DateTimeFormatter.ISO_OFFSET_DATE.parse(n.toString()));
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  private boolean notDateTime(Object n) {
    try {
      LocalDate.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(n.toString()));
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  private boolean notNumber(Object n) {
    return (n instanceof BigDecimal || n instanceof Integer ||
      n instanceof Float || n instanceof Double || n instanceof BigInteger);
  }
}
