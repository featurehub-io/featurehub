part of featurehub_client_api.api;

enum StrategyNameType { ATTRIBUTE, PERCENTAGE }

class StrategyNameTypeTypeTransformer {
  static Map<String, StrategyNameType> fromJsonMap = {
    'ATTRIBUTE': StrategyNameType.ATTRIBUTE,
    'PERCENTAGE': StrategyNameType.PERCENTAGE
  };
  static Map<StrategyNameType, String> toJsonMap = {
    StrategyNameType.ATTRIBUTE: 'ATTRIBUTE',
    StrategyNameType.PERCENTAGE: 'PERCENTAGE'
  };

  static StrategyNameType fromJson(dynamic data) {
    var found = fromJsonMap[data];
    if (found == null) {
      throw ('Unknown enum value to decode: $data');
    }
    return found;
  }

  static dynamic toJson(StrategyNameType data) {
    return toJsonMap[data];
  }

  static List<StrategyNameType> listFromJson(List<dynamic> json) {
    return json == null
        ? <StrategyNameType>[]
        : json.map((value) => fromJson(value)).toList();
  }

  static StrategyNameType copyWith(StrategyNameType instance) {
    return instance;
  }

  static Map<String, StrategyNameType> mapFromJson(Map<String, dynamic> json) {
    final map = <String, StrategyNameType>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) => map[key] = fromJson(value));
    }
    return map;
  }
}
