part of featurehub_client_api.api;

enum FeatureValueType { BOOLEAN, STRING, NUMBER, JSON }

class FeatureValueTypeTypeTransformer {
  static Map<String, FeatureValueType> fromJsonMap = {
    'BOOLEAN': FeatureValueType.BOOLEAN,
    'STRING': FeatureValueType.STRING,
    'NUMBER': FeatureValueType.NUMBER,
    'JSON': FeatureValueType.JSON
  };
  static Map<FeatureValueType, String> toJsonMap = {
    FeatureValueType.BOOLEAN: 'BOOLEAN',
    FeatureValueType.STRING: 'STRING',
    FeatureValueType.NUMBER: 'NUMBER',
    FeatureValueType.JSON: 'JSON'
  };

  static FeatureValueType fromJson(dynamic data) {
    var found = fromJsonMap[data];
    if (found == null) {
      throw ('Unknown enum value to decode: $data');
    }
    return found;
  }

  static dynamic toJson(FeatureValueType data) {
    return toJsonMap[data];
  }

  static List<FeatureValueType> listFromJson(List<dynamic> json) {
    return json == null
        ? <FeatureValueType>[]
        : json.map((value) => fromJson(value)).toList();
  }

  static FeatureValueType copyWith(FeatureValueType instance) {
    return instance;
  }

  static Map<String, FeatureValueType> mapFromJson(Map<String, dynamic> json) {
    final map = <String, FeatureValueType>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) => map[key] = fromJson(value));
    }
    return map;
  }
}
