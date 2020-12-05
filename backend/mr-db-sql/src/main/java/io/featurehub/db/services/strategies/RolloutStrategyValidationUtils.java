package io.featurehub.db.services.strategies;

import io.featurehub.db.api.RolloutStrategyValidator;
import io.featurehub.mr.model.RolloutStrategy;
import io.featurehub.mr.model.RolloutStrategyAttribute;
import io.featurehub.mr.model.RolloutStrategyCollectionViolationType;
import io.featurehub.mr.model.RolloutStrategyInstance;
import io.featurehub.mr.model.RolloutStrategyViolation;
import io.featurehub.mr.model.RolloutStrategyViolationType;
import io.featurehub.mr.model.StrategyAttributeWellKnownNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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


  @Override
  public ValidationFailure validateStrategies(List<RolloutStrategy> customStrategies,
                                              List<RolloutStrategyInstance> sharedStrategies) {
    return validateStrategies(customStrategies, sharedStrategies, null);
  }

  public ValidationFailure validateStrategies(List<RolloutStrategy> strategies,
                                              List<RolloutStrategyInstance> sharedStrategies,
                                              ValidationFailure failure) {

    ValidationFailure failures = failure == null ? new ValidationFailure() : failure;

    if ((strategies == null || strategies.isEmpty()) && (sharedStrategies == null || sharedStrategies.isEmpty())) {
      return failures;
    }

    // if any of them have no attributes and no percentage (or invalid percentage) then this is invalid
    // or no name or a name that is > 200 long
    strategies.forEach(rs -> {
      if (rs.getName() == null || rs.getName().isEmpty()) {
        failures.add(new RolloutStrategyViolation().violation(RolloutStrategyViolationType.NO_NAME), rs);
      } else if (rs.getName().trim().length() > 200) {
        failures.add(new RolloutStrategyViolation().violation(RolloutStrategyViolationType.NAME_TOO_LONG), rs);
      }

//      if (rs.getAttributes().isEmpty() && rs.getPercentage() == null) {
//        failures.add(new RolloutStrategyViolation().violation(RolloutStrategyViolationType.EMPTY_MATCH_CRITERIA), rs);
//      }

      if (rs.getPercentage() != null && rs.getPercentage() < 0) {
        failures.add(new RolloutStrategyViolation().violation(RolloutStrategyViolationType.NEGATIVE_PERCENTAGE), rs);
      }

      if (rs.getPercentage() != null && rs.getPercentage() > MAX_PERCENTAGE) {
        failures.add(new RolloutStrategyViolation().violation(RolloutStrategyViolationType.PERCENTAGE_OVER_100_PERCENT), rs);
      }

      rs.getAttributes()
        .stream()
        .filter(a -> a.getValues() == null || a.getValues().isEmpty())
        .forEach(a -> {
          failures.add(new RolloutStrategyViolation()
            .violation(RolloutStrategyViolationType.ARRAY_ATTRIBUTE_NO_VALUES)
            .id(a.getId()), rs);
        });

      if (!rs.getAttributes().isEmpty()) {
        Set<String> wellKnownFieldNames =
          Arrays.stream(StrategyAttributeWellKnownNames.values()).map(StrategyAttributeWellKnownNames::getValue).collect(Collectors.toSet());

        for (RolloutStrategyAttribute attr : rs.getAttributes()) {
          if (attr.getConditional() == null) {
            failures.add(new RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_MISSING_CONDITIONAL).id(attr.getId()), rs);
          }

          if (attr.getFieldName() == null) {
            failures.add(new RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_MISSING_FIELD_NAME).id(attr.getId()), rs);
          }

          if (attr.getType() == null) {
            failures.add(new RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_MISSING_FIELD_TYPE).id(attr.getId()), rs);
          }

          if (attr.getType() != null) {
            try {
              switch (attr.getType()) {
                case STRING:
                  attr.setValues(attr.getValues().stream().map(v -> v.toString().trim()).collect(Collectors.toList()));
                  break;
                case SEMANTIC_VERSION:
                  if (attr.getValues().stream().anyMatch(n -> n == null || notSemanticVersion(n))) {
                    failures.add(new RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_VAL_NOT_SEMANTIC_VERSION).id(attr.getId()), rs);
                  }
                  break;
                case NUMBER:
                  if (attr.getValues().stream().anyMatch(n -> n == null || notNumber(n))) {
                    failures.add(new RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_VAL_NOT_NUMBER).id(attr.getId()), rs);
                  }
                  break;
                case DATE:
                  if (attr.getValues().stream().anyMatch(n -> n == null || notDate(n))) {
                    failures.add(new RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_VAL_NOT_DATE).id(attr.getId()), rs);
                  }
                  break;
                case DATETIME:
                  if (attr.getValues().stream().anyMatch(n -> n == null || notDateTime(n))) {
                    failures.add(new RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_VAL_NOT_DATE_TIME).id(attr.getId()), rs);
                  }
                  break;
                case BOOLEAN:
                  attr.setValues(attr.getValues().stream().filter(Objects::nonNull)
                    .map(b -> Boolean.parseBoolean(b.toString())).collect(Collectors.toList()));
                  break;
                case IP_ADDRESS:
                  if (attr.getValues().stream().anyMatch(n -> n == null || notIpAddress(n))) {
                    failures.add(new RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_VAL_NOT_CIDR).id(attr.getId()), rs);
                  }
                  break;
              }
            } catch (Exception e) {
              log.warn("Failed to cleanup strategy {}", rs, e);
              failures.add(new RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_UNKNOWN_FAILURE).id(attr.getId()), rs);
            }
          }
        }
      }

    });


    // if total amount of > MAX_PERCENTAGE for only percentage strategies not allowed
    if (strategies.stream()
      .filter(rsi -> (rsi.getAttributes() == null || rsi.getAttributes().isEmpty()) && rsi.getPercentage() != null)
      .map(RolloutStrategy::getPercentage).reduce(0,
        Integer::sum) > MAX_PERCENTAGE) {
      failures.add(RolloutStrategyCollectionViolationType.PERCENTAGE_ADDS_OVER_100_PERCENT);
    }

    return failures;
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


}
