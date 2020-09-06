part of featurehub_client_api.api;

enum StrategyAttributePlatformName { linux, windows, macos, android, ios }

class StrategyAttributePlatformNameTypeTransformer {
  static Map<String, StrategyAttributePlatformName> fromJsonMap = {
    'linux': StrategyAttributePlatformName.linux,
    'windows': StrategyAttributePlatformName.windows,
    'macos': StrategyAttributePlatformName.macos,
    'android': StrategyAttributePlatformName.android,
    'ios': StrategyAttributePlatformName.ios
  };
  static Map<StrategyAttributePlatformName, String> toJsonMap = {
    StrategyAttributePlatformName.linux: 'linux',
    StrategyAttributePlatformName.windows: 'windows',
    StrategyAttributePlatformName.macos: 'macos',
    StrategyAttributePlatformName.android: 'android',
    StrategyAttributePlatformName.ios: 'ios'
  };

  static StrategyAttributePlatformName fromJson(dynamic data) {
    var found = fromJsonMap[data];
    if (found == null) {
      throw ('Unknown enum value to decode: $data');
    }
    return found;
  }

  static dynamic toJson(StrategyAttributePlatformName data) {
    return toJsonMap[data];
  }

  static List<StrategyAttributePlatformName> listFromJson(List<dynamic> json) {
    return json == null
        ? <StrategyAttributePlatformName>[]
        : json.map((value) => fromJson(value)).toList();
  }

  static StrategyAttributePlatformName copyWith(
      StrategyAttributePlatformName instance) {
    return instance;
  }

  static Map<String, StrategyAttributePlatformName> mapFromJson(
      Map<String, dynamic> json) {
    final map = <String, StrategyAttributePlatformName>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) => map[key] = fromJson(value));
    }
    return map;
  }
}
