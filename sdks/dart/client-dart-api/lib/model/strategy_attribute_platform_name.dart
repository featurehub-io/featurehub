part of featurehub_client_api.api;

enum StrategyAttributePlatformName { linux, windows, macos, android, ios }

extension StrategyAttributePlatformNameExtension
    on StrategyAttributePlatformName {
  String get name => toMap[this];

  // you have to call this extension class to use this as this is not yet supported
  static StrategyAttributePlatformName type(String name) => fromMap[name];

  static Map<String, StrategyAttributePlatformName> fromMap = {
    'linux': StrategyAttributePlatformName.linux,
    'windows': StrategyAttributePlatformName.windows,
    'macos': StrategyAttributePlatformName.macos,
    'android': StrategyAttributePlatformName.android,
    'ios': StrategyAttributePlatformName.ios
  };
  static Map<StrategyAttributePlatformName, String> toMap = {
    StrategyAttributePlatformName.linux: 'linux',
    StrategyAttributePlatformName.windows: 'windows',
    StrategyAttributePlatformName.macos: 'macos',
    StrategyAttributePlatformName.android: 'android',
    StrategyAttributePlatformName.ios: 'ios'
  };

  static StrategyAttributePlatformName fromJson(dynamic data) =>
      data == null ? null : fromMap[data];

  dynamic toJson() => toMap[this];

  static List<StrategyAttributePlatformName> listFromJson(List<dynamic> json) =>
      json == null
          ? <StrategyAttributePlatformName>[]
          : json.map((value) => fromJson(value)).toList();

  static StrategyAttributePlatformName copyWith(
          StrategyAttributePlatformName instance) =>
      instance;

  static Map<String, StrategyAttributePlatformName> mapFromJson(
      Map<String, dynamic> json) {
    final map = <String, StrategyAttributePlatformName>{};
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
