part of featurehub_client_api.api;

enum StrategyAttributeDeviceName { browser, mobile, desktop }

class StrategyAttributeDeviceNameTypeTransformer {
  static Map<String, StrategyAttributeDeviceName> fromJsonMap = {
    'browser': StrategyAttributeDeviceName.browser,
    'mobile': StrategyAttributeDeviceName.mobile,
    'desktop': StrategyAttributeDeviceName.desktop
  };
  static Map<StrategyAttributeDeviceName, String> toJsonMap = {
    StrategyAttributeDeviceName.browser: 'browser',
    StrategyAttributeDeviceName.mobile: 'mobile',
    StrategyAttributeDeviceName.desktop: 'desktop'
  };

  static StrategyAttributeDeviceName fromJson(dynamic data) {
    var found = fromJsonMap[data];
    if (found == null) {
      throw ('Unknown enum value to decode: $data');
    }
    return found;
  }

  static dynamic toJson(StrategyAttributeDeviceName data) {
    return toJsonMap[data];
  }

  static List<StrategyAttributeDeviceName> listFromJson(List<dynamic> json) {
    return json == null
        ? <StrategyAttributeDeviceName>[]
        : json.map((value) => fromJson(value)).toList();
  }

  static StrategyAttributeDeviceName copyWith(
      StrategyAttributeDeviceName instance) {
    return instance;
  }

  static Map<String, StrategyAttributeDeviceName> mapFromJson(
      Map<String, dynamic> json) {
    final map = <String, StrategyAttributeDeviceName>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) => map[key] = fromJson(value));
    }
    return map;
  }
}
