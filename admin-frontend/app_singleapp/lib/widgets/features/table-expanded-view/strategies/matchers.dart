import 'package:mrapi/api.dart';

List<RolloutStrategyAttributeConditional> defineMatchers(RolloutStrategyFieldType attributeType, StrategyAttributeWellKnownNames wellKnown) {

  final _equalsOnlyMatchers = <RolloutStrategyAttributeConditional>[
    RolloutStrategyAttributeConditional.EQUALS, RolloutStrategyAttributeConditional.NOT_EQUALS];

  final _semanticVersionMatchers = <RolloutStrategyAttributeConditional>[
    RolloutStrategyAttributeConditional.EQUALS, RolloutStrategyAttributeConditional.NOT_EQUALS,
    RolloutStrategyAttributeConditional.LESS, RolloutStrategyAttributeConditional.LESS_EQUALS,
    RolloutStrategyAttributeConditional.GREATER, RolloutStrategyAttributeConditional.GREATER_EQUALS];

  final _userkeyMatchers = <RolloutStrategyAttributeConditional>[
    RolloutStrategyAttributeConditional.EQUALS, RolloutStrategyAttributeConditional.NOT_EQUALS,
    RolloutStrategyAttributeConditional.ENDS_WITH, RolloutStrategyAttributeConditional.STARTS_WITH,
    RolloutStrategyAttributeConditional.REGEX,
    RolloutStrategyAttributeConditional.EXCLUDES, RolloutStrategyAttributeConditional.INCLUDES];

  final _stringMatchers = <RolloutStrategyAttributeConditional>[
    RolloutStrategyAttributeConditional.EQUALS, RolloutStrategyAttributeConditional.NOT_EQUALS,
    RolloutStrategyAttributeConditional.ENDS_WITH, RolloutStrategyAttributeConditional.STARTS_WITH,
    RolloutStrategyAttributeConditional.LESS, RolloutStrategyAttributeConditional.LESS_EQUALS,
    RolloutStrategyAttributeConditional.GREATER, RolloutStrategyAttributeConditional.GREATER_EQUALS,
    RolloutStrategyAttributeConditional.EXCLUDES, RolloutStrategyAttributeConditional.INCLUDES,
    RolloutStrategyAttributeConditional.REGEX,
  ];

  final _numberMatchers = <RolloutStrategyAttributeConditional>[
    RolloutStrategyAttributeConditional.EQUALS, RolloutStrategyAttributeConditional.NOT_EQUALS,
    RolloutStrategyAttributeConditional.ENDS_WITH, RolloutStrategyAttributeConditional.STARTS_WITH,
    RolloutStrategyAttributeConditional.LESS, RolloutStrategyAttributeConditional.LESS_EQUALS,
    RolloutStrategyAttributeConditional.GREATER, RolloutStrategyAttributeConditional.GREATER_EQUALS,
    RolloutStrategyAttributeConditional.EXCLUDES, RolloutStrategyAttributeConditional.INCLUDES,
  ];

  final _ipAddressMatchers = <RolloutStrategyAttributeConditional>[
    RolloutStrategyAttributeConditional.EQUALS, RolloutStrategyAttributeConditional.NOT_EQUALS,
    RolloutStrategyAttributeConditional.EXCLUDES, RolloutStrategyAttributeConditional.INCLUDES,
  ];

  final _dateMatchers = <RolloutStrategyAttributeConditional>[
    RolloutStrategyAttributeConditional.EQUALS, RolloutStrategyAttributeConditional.NOT_EQUALS,
    RolloutStrategyAttributeConditional.ENDS_WITH, RolloutStrategyAttributeConditional.STARTS_WITH,
    RolloutStrategyAttributeConditional.LESS, RolloutStrategyAttributeConditional.LESS_EQUALS,
    RolloutStrategyAttributeConditional.GREATER, RolloutStrategyAttributeConditional.GREATER_EQUALS,
    RolloutStrategyAttributeConditional.REGEX,
  ];


  if(wellKnown != null) {
    if (wellKnown == StrategyAttributeWellKnownNames.country ||
        wellKnown == StrategyAttributeWellKnownNames.device ||
        wellKnown == StrategyAttributeWellKnownNames.platform) {
      return _equalsOnlyMatchers;
    }

    if (wellKnown == StrategyAttributeWellKnownNames.version) {
      return _semanticVersionMatchers;
    }

    if (wellKnown == StrategyAttributeWellKnownNames.userkey) {
      return _userkeyMatchers;
    }
  }

  else if (attributeType == RolloutStrategyFieldType.BOOLEAN){
    return _equalsOnlyMatchers;
  }

  else if (attributeType == RolloutStrategyFieldType.STRING){
    return _stringMatchers;
  }

  else if (attributeType == RolloutStrategyFieldType.DATE || attributeType == RolloutStrategyFieldType.DATETIME){
    return _dateMatchers;
  }

  else if (attributeType == RolloutStrategyFieldType.NUMBER){
    return _numberMatchers;
  }

  else if (attributeType == RolloutStrategyFieldType.SEMANTIC_VERSION){
    return _semanticVersionMatchers;
  }

  else if (attributeType == RolloutStrategyFieldType.IP_ADDRESS){
    return _ipAddressMatchers;
  }

  return _equalsOnlyMatchers;
}
