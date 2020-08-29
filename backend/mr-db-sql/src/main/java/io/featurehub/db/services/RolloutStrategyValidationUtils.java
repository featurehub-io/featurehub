package io.featurehub.db.services;

import io.featurehub.db.api.RolloutStrategyValidator;
import io.featurehub.mr.model.RolloutStrategy;
import io.featurehub.mr.model.RolloutStrategyAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RolloutStrategyValidationUtils implements RolloutStrategyValidator {
  private static final Logger log = LoggerFactory.getLogger(RolloutStrategyValidationUtils.class);
  public static final int MAX_PERCENTAGE = 1000000; // four decimal points
  public static final Pattern semanticVersion = Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)" +
    "(?:-(" +
    "(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+" +
    "([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");
  public static final String v4 = "(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)(?:\\." +
    "(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)){3}";
  public static final String v4Cidr = v4 + "/(3[0-2]|[12]?[0-9])";
  public static final Pattern v4Ip = Pattern.compile(v4Cidr);
  public static final String v6Seg = "[a-fA-F\\d]{1,4}";
  public static final String v6 = ("(\n" +
    "(?:${v6seg}:){7}(?:${v6seg}|:)|  // 1:2:3:4:5:6:7::  1:2:3:4:5:6:7:8\n".replaceAll("\\s*//.*$", "") +
    ("(?:${v6seg}:){6}(?:${v4}|:${v6seg}|:)|  // 1:2:3:4:5:6::    1:2:3:4:5:6::8   " +
      "1:2:3:4:5:6::8  1:2:3:4:5:6::1.2.3.4\n").replaceAll("\\s*//.*$", "") +
    ("(?:${v6seg}:){5}(?::${v4}|(:${v6seg}){1,2}|:)|  // 1:2:3:4:5::      1:2:3:4:5::7:8   " +
      "1:2:3:4:5::8" +
      " 1:2:3:4:5::7:1.2.3.4\n").replaceAll("\\s*//.*$", "") +
    ("(?:${v6seg}:){4}(?:(:${v6seg}){0,1}:${v4}|(:${v6seg}){1,3}|:)| // 1:2:3:4::        1:2:3:4::6:7:8   1:2:3:4::8 " +
      " " +
      "    1:2:3:4::6:7:1.2.3.4\n").replaceAll("\\s*//.*$", "") +
    ("(?:${v6seg}:){3}(?:(:${v6seg}){0,2}:${v4}|(:${v6seg}){1,4}|:)| // 1:2:3::          1:2:3::5:6:7:8   1:2:3::8   " +
      " " +
      "    1:2:3::5:6:7:1.2.3.4\n").replaceAll("\\s*//.*$", "") +
    ("(?:${v6seg}:){2}(?:(:${v6seg}){0,3}:${v4}|(:${v6seg}){1,5}|:)| // 1:2::            1:2::4:5:6:7:8   1:2::8     " +
      " " +
      "    1:2::4:5:6:7:1.2.3.4\n").replaceAll("\\s*//.*$", "") +
    ("(?:${v6seg}:){1}(?:(:${v6seg}){0,4}:${v4}|(:${v6seg}){1,6}|:)| // 1::              1::3:4:5:6:7:8   1::8       " +
      " " +
      "    1::3:4:5:6:7:1.2.3.4\n").replaceAll("\\s*//.*$", "") +
    ("(?::((?::${v6seg}){0,5}:${v4}|(?::${v6seg}){1,7}|:))           // ::2:3:4:5:6:7:8  ::2:3:4:5:6:7:8  ::8        " +
      " " +
      "    ::1.2.3.4\n").replaceAll("\\s*//.*$", "") +
    ")(%[0-9a-zA-Z]{1,})?    // %eth0            %1\n").replaceAll("\\s*//.*$", "")
    .replace("${v6seg}", v6Seg)
    .replace("${v4}", v4)
    .replace("\n", "");
  public static final String v6Cidr = v6 + "/(12[0-8]|1[01][0-9]|[1-9]?[0-9])";

  public static final Pattern v6Ip = Pattern.compile(v6Cidr);


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
                  if (attr.getValues().stream().anyMatch(n -> n == null || notSemanticVersion(n))) {
                    log.warn("one of strategy is not a semantic version: {}", attr);
                    throw new InvalidStrategyCombination();
                  }
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
                    throw new InvalidStrategyCombination();
                  }
                  break;
                case DATETIME:
                  if (attr.getValues().stream().anyMatch(n -> n == null || notDateTime(n))) {
                    log.warn("one of strategy values is not a date: {}", attr);
                    throw new InvalidStrategyCombination();
                  }
                  break;
                case BOOLEAN:
                  attr.setValues(attr.getValues().stream().filter(Objects::nonNull)
                    .map(b -> Boolean.parseBoolean(b.toString())).collect(Collectors.toList()));
                  break;
                case IP_ADDRESS:
                  if (attr.getValues().stream().anyMatch(n -> n == null || notIpAddress(n))) {
                    log.warn("one of strategy is not a v4 or v6 address or CIDR block: {}", attr);
                    throw new InvalidStrategyCombination();
                  }
                  break;
              }
            } catch (Exception e) {
              log.warn("Failed to cleanup strategy {}", rs, e);
              throw new InvalidStrategyCombination();
            }
          } else {
            attr.getValues().clear();
            if (attr.getValue() != null) {
              switch (attr.getType()) {
                case STRING:
                  break;
                case SEMANTIC_VERSION:
                  if (notSemanticVersion(attr.getValue())) {
                    log.warn("one of strategy is not a semantic version: {}", attr);
                    throw new InvalidStrategyCombination();
                  }
                  break;
                case NUMBER:
                  if (notNumber(attr.getValue())) {
                    log.warn("one of strategy values is not a number: {}", attr);
                    throw new InvalidStrategyCombination();
                  }
                  break;
                case DATE:
                  if (notDate(attr.getValue())) {
                    log.warn("one of strategy values is not a date: {}", attr);
                    throw new InvalidStrategyCombination();
                  }
                  break;
                case DATETIME:
                  if (notDateTime(attr.getValue())) {
                    log.warn("one of strategy values is not a date-time: {}", attr);
                    throw new InvalidStrategyCombination();
                  }
                  break;
                case BOOLEAN:
                  break;
                case IP_ADDRESS:
                  if (notIpAddress(attr.getValue())) {
                    log.warn("one of strategy values is not a ip address: {}", attr);
                    throw new InvalidStrategyCombination();
                  }
                  break;
              }
            }
          }
        }
      }
    }
  }

  private boolean notIpAddress(Object n) {
    try {
      String input = n.toString();
      if (!input.contains("/")) {
        input = input + "/0";
      }
      return !v4Ip.matcher(input).matches() && !v6Ip.matcher(input).matches();
    } catch (Exception ignored) {
      return true;
    }
  }

  private boolean notSemanticVersion(Object n) {
    try {
      return !semanticVersion.matcher(n.toString()).matches();
    } catch (Exception ignored) {
      return true;
    }
  }

  private boolean notDate(Object n) {
    try {
      LocalDate.from(DateTimeFormatter.ISO_DATE.parse(n.toString()));
      return false;
    } catch (Exception ignored) {
      return true;
    }
  }

  private boolean notDateTime(Object n) {
    try {
      OffsetDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(n.toString()));
      return false;
    } catch (Exception ignored) {
      return true;
    }
  }

  private boolean notNumber(Object n) {
    return !(n instanceof BigDecimal || n instanceof Integer ||
      n instanceof Float || n instanceof Double || n instanceof BigInteger);
  }

  public static void main(String[] args) {
    System.out.println(v6);
  }
}
