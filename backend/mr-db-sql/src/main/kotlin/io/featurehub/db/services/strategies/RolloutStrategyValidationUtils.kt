package io.featurehub.db.services.strategies

import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.mr.model.*
import org.slf4j.LoggerFactory
import java.lang.Integer.sum
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern

class RolloutStrategyValidationUtils : RolloutStrategyValidator {
  override fun validateStrategies(
    featureValueType: FeatureValueType?,
    customStrategies: List<RolloutStrategy>,
    sharedStrategies: List<RolloutStrategyInstance>
  ): RolloutStrategyValidator.ValidationFailure {
    return validateStrategies(featureValueType, customStrategies, sharedStrategies, null)
  }

  override fun validateStrategies(
    featureValueType: FeatureValueType?,
    customStrategies: List<RolloutStrategy>,
    sharedStrategies: List<RolloutStrategyInstance>,
    failure: RolloutStrategyValidator.ValidationFailure?
  ): RolloutStrategyValidator.ValidationFailure {
    val failures = failure ?: RolloutStrategyValidator.ValidationFailure()
    if (customStrategies.isEmpty() && sharedStrategies.isEmpty()) {
      return failures
    }
    if (featureValueType == FeatureValueType.BOOLEAN) {
      if (customStrategies.stream().anyMatch { i: RolloutStrategy -> i.value == null }) {
        failures.add(RolloutStrategyCollectionViolationType.BOOLEAN_HAS_NO_VALUE)
      }
    }
    customStrategies.stream().anyMatch { i: RolloutStrategy -> i.value == null }

    // if any of them have no attributes and no percentage (or invalid percentage) then this is invalid
    // or no name or a name that is > 200 long
    customStrategies.forEach { rs: RolloutStrategy ->
      if (rs.name == null || rs.name.isEmpty()) {
        failures.add(RolloutStrategyViolation().violation(RolloutStrategyViolationType.NO_NAME), rs)
      } else if (rs.name.trim { it <= ' ' }.length > 200) {
        failures.add(RolloutStrategyViolation().violation(RolloutStrategyViolationType.NAME_TOO_LONG), rs)
      }

//      if (rs.getAttributes().isEmpty() && rs.getPercentage() == null) {
//        failures.add(new RolloutStrategyViolation().violation(RolloutStrategyViolationType.EMPTY_MATCH_CRITERIA), rs);
//      }
      if (rs.percentage != null && rs.percentage!! < 0) {
        failures.add(RolloutStrategyViolation().violation(RolloutStrategyViolationType.NEGATIVE_PERCENTAGE), rs)
      }
      if (rs.percentage != null && rs.percentage!! > MAX_PERCENTAGE) {
        failures.add(
          RolloutStrategyViolation().violation(RolloutStrategyViolationType.PERCENTAGE_OVER_100_PERCENT),
          rs
        )
      }
      rs.attributes?.filter { a: RolloutStrategyAttribute ->
        a.values == null || a.values.isEmpty()
      }?.forEach { a: RolloutStrategyAttribute ->
        failures.add(
          RolloutStrategyViolation()
            .violation(RolloutStrategyViolationType.ARRAY_ATTRIBUTE_NO_VALUES)
            .id(a.id), rs
        )
      }

      if (rs.attributes?.isNotEmpty() == true) {
//        val wellKnownFieldNames = StrategyAttributeWellKnownNames.values()
//          .map { obj: StrategyAttributeWellKnownNames -> obj.value!! }.toSet()

        for (attr in rs.attributes!!) {
          if (attr.conditional == null) {
            failures.add(
              RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_MISSING_CONDITIONAL)
                .id(attr.id), rs
            )
          }
          if (attr.fieldName == null) {
            failures.add(
              RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_MISSING_FIELD_NAME)
                .id(attr.id), rs
            )
          }
          if (attr.type == null) {
            failures.add(
              RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_MISSING_FIELD_TYPE)
                .id(attr.id), rs
            )
          }
          if (attr.type != null) {
            try {
              when (attr.type) {
                RolloutStrategyFieldType.STRING ->
                  attr.values = attr.values.map { v: Any -> v.toString().trim { it <= ' ' } }

                RolloutStrategyFieldType.SEMANTIC_VERSION ->
                  if (attr.values.any { n -> n == null || notSemanticVersion(n) }) {
                    failures.add(
                      RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_VAL_NOT_SEMANTIC_VERSION)
                        .id(attr.id), rs
                    )
                  }

                RolloutStrategyFieldType.NUMBER -> if (attr.values.any { n -> n == null || notNumber(n) }) {
                  failures.add(
                    RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_VAL_NOT_NUMBER)
                      .id(attr.id), rs
                  )
                }

                RolloutStrategyFieldType.DATE -> if (attr.values.any { n -> n == null || notDate(n) }) {
                  failures.add(
                    RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_VAL_NOT_DATE)
                      .id(attr.id), rs
                  )
                }

                RolloutStrategyFieldType.DATETIME -> if (attr.values.any { n -> n == null || notDateTime(n) }) {
                  failures.add(
                    RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_VAL_NOT_DATE_TIME)
                      .id(attr.id), rs
                  )
                }

                RolloutStrategyFieldType.BOOLEAN -> attr.values = attr.values.filter { obj -> Objects.nonNull(obj) }
                    .map { b -> java.lang.Boolean.parseBoolean(b.toString()) }

                RolloutStrategyFieldType.IP_ADDRESS -> if (attr.values.any { n -> n == null || notIpAddress(n) }) {
                  failures.add(
                    RolloutStrategyViolation().violation(RolloutStrategyViolationType.ATTR_VAL_NOT_CIDR)
                      .id(attr.id), rs
                  )
                }

                else -> {}
              }
            } catch (e: Exception) {
              log.warn("Failed to cleanup strategy {}", rs, e)
              failures.add(RolloutStrategyViolation()
                .violation(RolloutStrategyViolationType.ATTR_UNKNOWN_FAILURE).id(attr.id), rs)
            }
          }
        }
      }
    }


    // if total amount of > MAX_PERCENTAGE for only percentage strategies not allowed
    if ((customStrategies
        .filter { rsi: RolloutStrategy -> (rsi.attributes == null || rsi.attributes!!.isEmpty()) && rsi.percentage != null }
        .map { obj: RolloutStrategy -> obj.percentage!! }
        .reduceOrNull { a, b -> sum(a, b) } ?: 0) > MAX_PERCENTAGE
    ) {
      failures.add(RolloutStrategyCollectionViolationType.PERCENTAGE_ADDS_OVER_100_PERCENT)
    }
    return failures
  }

  private fun notIpAddress(n: Any): Boolean {
    return try {
      var input = n.toString()
      if (!input.contains("/")) {
        input = "$input/0"
      }
      !v4Ip.matcher(input).matches() && !v6Ip.matcher(input).matches()
    } catch (ignored: Exception) {
      true
    }
  }

  private fun notSemanticVersion(n: Any): Boolean {
    return try {
      !semanticVersion.matcher(n.toString()).matches()
    } catch (ignored: Exception) {
      true
    }
  }

  private fun notDate(n: Any): Boolean {
    return try {
      LocalDate.from(DateTimeFormatter.ISO_DATE.parse(n.toString()))
      false
    } catch (ignored: Exception) {
      true
    }
  }

  private fun notDateTime(n: Any): Boolean {
    return try {
      OffsetDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(n.toString()))
      false
    } catch (ignored: Exception) {
      true
    }
  }

  private fun notNumber(n: Any): Boolean {
    return !(n is BigDecimal || n is Int ||
      n is Float || n is Double || n is BigInteger)
  }

  companion object {
    private val log = LoggerFactory.getLogger(RolloutStrategyValidationUtils::class.java)
    private const val MAX_PERCENTAGE = 1000000 // four decimal points
    private val semanticVersion = Pattern.compile(
      "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)" +
        "(?:-(" +
        "(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+" +
        "([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$"
    )!!
    private const val v4 = "(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)(?:\\." +
      "(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)){3}"
    private const val v4Cidr = "$v4/(3[0-2]|[12]?[0-9])"
    private val v4Ip = Pattern.compile(v4Cidr)
    private const val v6Seg = "[a-fA-F\\d]{1,4}"
    private val v6 = ("""
    (
    ${"(?:\${v6seg}:){7}(?:\${v6seg}|:)|  // 1:2:3:4:5:6:7::  1:2:3:4:5:6:7:8\n".replace("\\s*//.*$".toRegex(), "")}
    """.trimIndent() +
      """(?:${"$"}{v6seg}:){6}(?:${"$"}{v4}|:${"$"}{v6seg}|:)|  // 1:2:3:4:5:6::    1:2:3:4:5:6::8   1:2:3:4:5:6::8  1:2:3:4:5:6::1.2.3.4
""".replace("\\s*//.*$".toRegex(), "") +
      """(?:${"$"}{v6seg}:){5}(?::${"$"}{v4}|(:${"$"}{v6seg}){1,2}|:)|  // 1:2:3:4:5::      1:2:3:4:5::7:8   1:2:3:4:5::8 1:2:3:4:5::7:1.2.3.4
""".replace("\\s*//.*$".toRegex(), "") +
      """(?:${"$"}{v6seg}:){4}(?:(:${"$"}{v6seg}){0,1}:${"$"}{v4}|(:${"$"}{v6seg}){1,3}|:)| // 1:2:3:4::        1:2:3:4::6:7:8   1:2:3:4::8      1:2:3:4::6:7:1.2.3.4
""".replace("\\s*//.*$".toRegex(), "") +
      """(?:${"$"}{v6seg}:){3}(?:(:${"$"}{v6seg}){0,2}:${"$"}{v4}|(:${"$"}{v6seg}){1,4}|:)| // 1:2:3::          1:2:3::5:6:7:8   1:2:3::8        1:2:3::5:6:7:1.2.3.4
""".replace("\\s*//.*$".toRegex(), "") +
      """(?:${"$"}{v6seg}:){2}(?:(:${"$"}{v6seg}){0,3}:${"$"}{v4}|(:${"$"}{v6seg}){1,5}|:)| // 1:2::            1:2::4:5:6:7:8   1:2::8          1:2::4:5:6:7:1.2.3.4
""".replace("\\s*//.*$".toRegex(), "") +
      """(?:${"$"}{v6seg}:){1}(?:(:${"$"}{v6seg}){0,4}:${"$"}{v4}|(:${"$"}{v6seg}){1,6}|:)| // 1::              1::3:4:5:6:7:8   1::8            1::3:4:5:6:7:1.2.3.4
""".replace("\\s*//.*$".toRegex(), "") +
      """(?::((?::${"$"}{v6seg}){0,5}:${"$"}{v4}|(?::${"$"}{v6seg}){1,7}|:))           // ::2:3:4:5:6:7:8  ::2:3:4:5:6:7:8  ::8             ::1.2.3.4
""".replace("\\s*//.*$".toRegex(), "") +
      ")(%[0-9a-zA-Z]{1,})?    // %eth0            %1\n").replace("\\s*//.*$".toRegex(), "")
      .replace("\${v6seg}", v6Seg)
      .replace("\${v4}", v4)
      .replace("\n", "")
    private val v6Cidr = "$v6/(12[0-8]|1[01][0-9]|[1-9]?[0-9])"
    private val v6Ip = Pattern.compile(v6Cidr)!!
  }

}
