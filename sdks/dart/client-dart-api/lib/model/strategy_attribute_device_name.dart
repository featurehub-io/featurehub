part of featurehub_client_api.api;

enum StrategyAttributeDeviceName {
  browser,
  mobile,
  desktop,
  server,
  watch,
  embedded
}

extension StrategyAttributeDeviceNameExtension on StrategyAttributeDeviceName {
  String get name => toMap[this];

  // you have to call this extension class to use this as this is not yet supported
  static StrategyAttributeDeviceName type(String name) => fromMap[name];

  static Map<String, StrategyAttributeDeviceName> fromMap = {
    'browser': StrategyAttributeDeviceName.browser,
    'mobile': StrategyAttributeDeviceName.mobile,
    'desktop': StrategyAttributeDeviceName.desktop,
    'server': StrategyAttributeDeviceName.server,
    'watch': StrategyAttributeDeviceName.watch,
    'embedded': StrategyAttributeDeviceName.embedded
  };
  static Map<StrategyAttributeDeviceName, String> toMap = {
    StrategyAttributeDeviceName.browser: 'browser',
    StrategyAttributeDeviceName.mobile: 'mobile',
    StrategyAttributeDeviceName.desktop: 'desktop',
    StrategyAttributeDeviceName.server: 'server',
    StrategyAttributeDeviceName.watch: 'watch',
    StrategyAttributeDeviceName.embedded: 'embedded'
  };

  static StrategyAttributeDeviceName fromJson(dynamic data) =>
      data == null ? null : fromMap[data];

  dynamic toJson() => toMap[this];

  static List<StrategyAttributeDeviceName> listFromJson(List<dynamic> json) =>
      json == null
          ? <StrategyAttributeDeviceName>[]
          : json.map((value) => fromJson(value)).toList();

  static StrategyAttributeDeviceName copyWith(
          StrategyAttributeDeviceName instance) =>
      instance;

  static Map<String, StrategyAttributeDeviceName> mapFromJson(
      Map<String, dynamic> json) {
    final map = <String, StrategyAttributeDeviceName>{};
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
