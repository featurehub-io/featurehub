part of featurehub_client_api.api;

enum FeatureValueType { BOOLEAN, STRING, NUMBER, JSON }

extension FeatureValueTypeExtension on FeatureValueType {
  String get name => toMap[this];

  // you have to call this extension class to use this as this is not yet supported
  static FeatureValueType type(String name) => fromMap[name];

  static Map<String, FeatureValueType> fromMap = {
    'BOOLEAN': FeatureValueType.BOOLEAN,
    'STRING': FeatureValueType.STRING,
    'NUMBER': FeatureValueType.NUMBER,
    'JSON': FeatureValueType.JSON
  };
  static Map<FeatureValueType, String> toMap = {
    FeatureValueType.BOOLEAN: 'BOOLEAN',
    FeatureValueType.STRING: 'STRING',
    FeatureValueType.NUMBER: 'NUMBER',
    FeatureValueType.JSON: 'JSON'
  };

  static FeatureValueType fromJson(dynamic data) =>
      data == null ? null : fromMap[data];

  dynamic toJson() => toMap[this];

  static List<FeatureValueType> listFromJson(List<dynamic> json) => json == null
      ? <FeatureValueType>[]
      : json.map((value) => fromJson(value)).toList();

  static FeatureValueType copyWith(FeatureValueType instance) => instance;

  static Map<String, FeatureValueType> mapFromJson(Map<String, dynamic> json) {
    final map = <String, FeatureValueType>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) => map[key] = fromJson(value));
    }
    return map;
  }
}
