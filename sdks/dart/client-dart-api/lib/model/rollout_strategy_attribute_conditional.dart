part of featurehub_client_api.api;

enum RolloutStrategyAttributeConditional {
  EQUALS,
  ENDS_WITH,
  STARTS_WITH,
  GREATER,
  GREATER_EQUALS,
  LESS,
  LESS_EQUALS,
  NOT_EQUALS,
  INCLUDES,
  EXCLUDES,
  REGEX
}

extension RolloutStrategyAttributeConditionalExtension
    on RolloutStrategyAttributeConditional {
  String get name => toMap[this];

  // you have to call this extension class to use this as this is not yet supported
  static RolloutStrategyAttributeConditional type(String name) => fromMap[name];

  static Map<String, RolloutStrategyAttributeConditional> fromMap = {
    'EQUALS': RolloutStrategyAttributeConditional.EQUALS,
    'ENDS_WITH': RolloutStrategyAttributeConditional.ENDS_WITH,
    'STARTS_WITH': RolloutStrategyAttributeConditional.STARTS_WITH,
    'GREATER': RolloutStrategyAttributeConditional.GREATER,
    'GREATER_EQUALS': RolloutStrategyAttributeConditional.GREATER_EQUALS,
    'LESS': RolloutStrategyAttributeConditional.LESS,
    'LESS_EQUALS': RolloutStrategyAttributeConditional.LESS_EQUALS,
    'NOT_EQUALS': RolloutStrategyAttributeConditional.NOT_EQUALS,
    'INCLUDES': RolloutStrategyAttributeConditional.INCLUDES,
    'EXCLUDES': RolloutStrategyAttributeConditional.EXCLUDES,
    'REGEX': RolloutStrategyAttributeConditional.REGEX
  };
  static Map<RolloutStrategyAttributeConditional, String> toMap = {
    RolloutStrategyAttributeConditional.EQUALS: 'EQUALS',
    RolloutStrategyAttributeConditional.ENDS_WITH: 'ENDS_WITH',
    RolloutStrategyAttributeConditional.STARTS_WITH: 'STARTS_WITH',
    RolloutStrategyAttributeConditional.GREATER: 'GREATER',
    RolloutStrategyAttributeConditional.GREATER_EQUALS: 'GREATER_EQUALS',
    RolloutStrategyAttributeConditional.LESS: 'LESS',
    RolloutStrategyAttributeConditional.LESS_EQUALS: 'LESS_EQUALS',
    RolloutStrategyAttributeConditional.NOT_EQUALS: 'NOT_EQUALS',
    RolloutStrategyAttributeConditional.INCLUDES: 'INCLUDES',
    RolloutStrategyAttributeConditional.EXCLUDES: 'EXCLUDES',
    RolloutStrategyAttributeConditional.REGEX: 'REGEX'
  };

  static RolloutStrategyAttributeConditional fromJson(dynamic data) =>
      data == null ? null : fromMap[data];

  dynamic toJson() => toMap[this];

  static List<RolloutStrategyAttributeConditional> listFromJson(
          List<dynamic> json) =>
      json == null
          ? <RolloutStrategyAttributeConditional>[]
          : json.map((value) => fromJson(value)).toList();

  static RolloutStrategyAttributeConditional copyWith(
          RolloutStrategyAttributeConditional instance) =>
      instance;

  static Map<String, RolloutStrategyAttributeConditional> mapFromJson(
      Map<String, dynamic> json) {
    final map = <String, RolloutStrategyAttributeConditional>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) {
        final val = fromJson(value);
        if (val != null) {
          map[key] = val;
        }
      });
    }
    return map;
  }
}
