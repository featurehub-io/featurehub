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

extension RolloutStrategyFieldTypeExtension on RolloutStrategyFieldType {
  String get name => toMap[this];

  // you have to call this extension class to use this as this is not yet supported
  static RolloutStrategyFieldType type(String name) => fromMap[name];

  static Map<String, RolloutStrategyFieldType> fromMap = {
    'STRING': RolloutStrategyFieldType.STRING,
    'SEMANTIC_VERSION': RolloutStrategyFieldType.SEMANTIC_VERSION,
    'NUMBER': RolloutStrategyFieldType.NUMBER,
    'DATE': RolloutStrategyFieldType.DATE,
    'DATETIME': RolloutStrategyFieldType.DATETIME,
    'BOOLEAN': RolloutStrategyFieldType.BOOLEAN,
    'IP_ADDRESS': RolloutStrategyFieldType.IP_ADDRESS
  };
  static Map<RolloutStrategyFieldType, String> toMap = {
    RolloutStrategyFieldType.STRING: 'STRING',
    RolloutStrategyFieldType.SEMANTIC_VERSION: 'SEMANTIC_VERSION',
    RolloutStrategyFieldType.NUMBER: 'NUMBER',
    RolloutStrategyFieldType.DATE: 'DATE',
    RolloutStrategyFieldType.DATETIME: 'DATETIME',
    RolloutStrategyFieldType.BOOLEAN: 'BOOLEAN',
    RolloutStrategyFieldType.IP_ADDRESS: 'IP_ADDRESS'
  };

  static RolloutStrategyFieldType fromJson(dynamic data) =>
      data == null ? null : fromMap[data];

  dynamic toJson() => toMap[this];

  static List<RolloutStrategyFieldType> listFromJson(List<dynamic> json) =>
      json == null
          ? <RolloutStrategyFieldType>[]
          : json.map((value) => fromJson(value)).toList();

  static RolloutStrategyFieldType copyWith(RolloutStrategyFieldType instance) =>
      instance;

  static Map<String, RolloutStrategyFieldType> mapFromJson(
      Map<String, dynamic> json) {
    final map = <String, RolloutStrategyFieldType>{};
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
