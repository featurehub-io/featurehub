part of featurehub_client_api.api;

enum RolloutStrategyFieldType {
  STRING,
  SEMANTIC_VERSION,
  NUMBER,
  DATE,
  DATETIME,
  BOOLEAN,
  IP_ADDRESS
}

class RolloutStrategyFieldTypeTypeTransformer {
  static Map<String, RolloutStrategyFieldType> fromJsonMap = {
    'STRING': RolloutStrategyFieldType.STRING,
    'SEMANTIC_VERSION': RolloutStrategyFieldType.SEMANTIC_VERSION,
    'NUMBER': RolloutStrategyFieldType.NUMBER,
    'DATE': RolloutStrategyFieldType.DATE,
    'DATETIME': RolloutStrategyFieldType.DATETIME,
    'BOOLEAN': RolloutStrategyFieldType.BOOLEAN,
    'IP_ADDRESS': RolloutStrategyFieldType.IP_ADDRESS
  };
  static Map<RolloutStrategyFieldType, String> toJsonMap = {
    RolloutStrategyFieldType.STRING: 'STRING',
    RolloutStrategyFieldType.SEMANTIC_VERSION: 'SEMANTIC_VERSION',
    RolloutStrategyFieldType.NUMBER: 'NUMBER',
    RolloutStrategyFieldType.DATE: 'DATE',
    RolloutStrategyFieldType.DATETIME: 'DATETIME',
    RolloutStrategyFieldType.BOOLEAN: 'BOOLEAN',
    RolloutStrategyFieldType.IP_ADDRESS: 'IP_ADDRESS'
  };

  static RolloutStrategyFieldType fromJson(dynamic data) {
    var found = fromJsonMap[data];
    if (found == null) {
      throw ('Unknown enum value to decode: $data');
    }
    return found;
  }

  static dynamic toJson(RolloutStrategyFieldType data) {
    return toJsonMap[data];
  }

  static List<RolloutStrategyFieldType> listFromJson(List<dynamic> json) {
    return json == null
        ? <RolloutStrategyFieldType>[]
        : json.map((value) => fromJson(value)).toList();
  }

  static RolloutStrategyFieldType copyWith(RolloutStrategyFieldType instance) {
    return instance;
  }

  static Map<String, RolloutStrategyFieldType> mapFromJson(
      Map<String, dynamic> json) {
    final map = <String, RolloutStrategyFieldType>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) => map[key] = fromJson(value));
    }
    return map;
  }
}
