import 'package:mrapi/api.dart';

List<RolloutStrategyAttributeConditional> defineMatchers(
    RolloutStrategyFieldType? attributeType,
    StrategyAttributeWellKnownNames? wellKnown) {
  final equalsOnlyMatchers = <RolloutStrategyAttributeConditional>[
    RolloutStrategyAttributeConditional.EQUALS,
    RolloutStrategyAttributeConditional.NOT_EQUALS,
    RolloutStrategyAttributeConditional.EXCLUDES,
    RolloutStrategyAttributeConditional.INCLUDES
  ];

  final semanticVersionMatchers = <RolloutStrategyAttributeConditional>[
    RolloutStrategyAttributeConditional.EQUALS,
    RolloutStrategyAttributeConditional.NOT_EQUALS,
    RolloutStrategyAttributeConditional.LESS,
    RolloutStrategyAttributeConditional.LESS_EQUALS,
    RolloutStrategyAttributeConditional.GREATER,
    RolloutStrategyAttributeConditional.GREATER_EQUALS
  ];

  final userkeyMatchers = <RolloutStrategyAttributeConditional>[
    RolloutStrategyAttributeConditional.EQUALS,
    RolloutStrategyAttributeConditional.NOT_EQUALS,
    RolloutStrategyAttributeConditional.ENDS_WITH,
    RolloutStrategyAttributeConditional.STARTS_WITH,
    RolloutStrategyAttributeConditional.REGEX,
    RolloutStrategyAttributeConditional.EXCLUDES,
    RolloutStrategyAttributeConditional.INCLUDES
  ];

  final stringMatchers = <RolloutStrategyAttributeConditional>[
    RolloutStrategyAttributeConditional.EQUALS,
    RolloutStrategyAttributeConditional.NOT_EQUALS,
    RolloutStrategyAttributeConditional.ENDS_WITH,
    RolloutStrategyAttributeConditional.STARTS_WITH,
    RolloutStrategyAttributeConditional.LESS,
    RolloutStrategyAttributeConditional.LESS_EQUALS,
    RolloutStrategyAttributeConditional.GREATER,
    RolloutStrategyAttributeConditional.GREATER_EQUALS,
    RolloutStrategyAttributeConditional.EXCLUDES,
    RolloutStrategyAttributeConditional.INCLUDES,
    RolloutStrategyAttributeConditional.REGEX,
  ];

  final numberMatchers = <RolloutStrategyAttributeConditional>[
    RolloutStrategyAttributeConditional.EQUALS,
    RolloutStrategyAttributeConditional.NOT_EQUALS,
    RolloutStrategyAttributeConditional.ENDS_WITH,
    RolloutStrategyAttributeConditional.STARTS_WITH,
    RolloutStrategyAttributeConditional.LESS,
    RolloutStrategyAttributeConditional.LESS_EQUALS,
    RolloutStrategyAttributeConditional.GREATER,
    RolloutStrategyAttributeConditional.GREATER_EQUALS,
    RolloutStrategyAttributeConditional.EXCLUDES,
    RolloutStrategyAttributeConditional.INCLUDES,
  ];

  final ipAddressMatchers = <RolloutStrategyAttributeConditional>[
    RolloutStrategyAttributeConditional.EQUALS,
    RolloutStrategyAttributeConditional.NOT_EQUALS,
    RolloutStrategyAttributeConditional.EXCLUDES,
    RolloutStrategyAttributeConditional.INCLUDES,
  ];

  final dateMatchers = <RolloutStrategyAttributeConditional>[
    RolloutStrategyAttributeConditional.EQUALS,
    RolloutStrategyAttributeConditional.NOT_EQUALS,
    RolloutStrategyAttributeConditional.ENDS_WITH,
    RolloutStrategyAttributeConditional.STARTS_WITH,
    RolloutStrategyAttributeConditional.LESS,
    RolloutStrategyAttributeConditional.LESS_EQUALS,
    RolloutStrategyAttributeConditional.GREATER,
    RolloutStrategyAttributeConditional.GREATER_EQUALS,
    RolloutStrategyAttributeConditional.REGEX,
  ];

  if (wellKnown != null) {
    if (wellKnown == StrategyAttributeWellKnownNames.country ||
        wellKnown == StrategyAttributeWellKnownNames.device ||
        wellKnown == StrategyAttributeWellKnownNames.platform) {
      return equalsOnlyMatchers;
    }

    if (wellKnown == StrategyAttributeWellKnownNames.version) {
      return semanticVersionMatchers;
    }

    if (wellKnown == StrategyAttributeWellKnownNames.userkey) {
      return userkeyMatchers;
    }
  } else if (attributeType == RolloutStrategyFieldType.BOOLEAN) {
    return equalsOnlyMatchers;
  } else if (attributeType == RolloutStrategyFieldType.STRING) {
    return stringMatchers;
  } else if (attributeType == RolloutStrategyFieldType.DATE ||
      attributeType == RolloutStrategyFieldType.DATETIME) {
    return dateMatchers;
  } else if (attributeType == RolloutStrategyFieldType.NUMBER) {
    return numberMatchers;
  } else if (attributeType == RolloutStrategyFieldType.SEMANTIC_VERSION) {
    return semanticVersionMatchers;
  } else if (attributeType == RolloutStrategyFieldType.IP_ADDRESS) {
    return ipAddressMatchers;
  }

  return equalsOnlyMatchers;
}
