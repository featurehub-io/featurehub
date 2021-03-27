part of featurehub_client_api.api;

enum StrategyAttributeWellKnownNames {
  device,
  country,
  platform,
  userkey,
  session,
  version
}

extension StrategyAttributeWellKnownNamesExtension
    on StrategyAttributeWellKnownNames {
  String get name => toMap[this];

  // you have to call this extension class to use this as this is not yet supported
  static StrategyAttributeWellKnownNames type(String name) => fromMap[name];

  static Map<String, StrategyAttributeWellKnownNames> fromMap = {
    'device': StrategyAttributeWellKnownNames.device,
    'country': StrategyAttributeWellKnownNames.country,
    'platform': StrategyAttributeWellKnownNames.platform,
    'userkey': StrategyAttributeWellKnownNames.userkey,
    'session': StrategyAttributeWellKnownNames.session,
    'version': StrategyAttributeWellKnownNames.version
  };
  static Map<StrategyAttributeWellKnownNames, String> toMap = {
    StrategyAttributeWellKnownNames.device: 'device',
    StrategyAttributeWellKnownNames.country: 'country',
    StrategyAttributeWellKnownNames.platform: 'platform',
    StrategyAttributeWellKnownNames.userkey: 'userkey',
    StrategyAttributeWellKnownNames.session: 'session',
    StrategyAttributeWellKnownNames.version: 'version'
  };

  static StrategyAttributeWellKnownNames fromJson(dynamic data) =>
      data == null ? null : fromMap[data];

  dynamic toJson() => toMap[this];

  static List<StrategyAttributeWellKnownNames> listFromJson(
          List<dynamic> json) =>
      json == null
          ? <StrategyAttributeWellKnownNames>[]
          : json.map((value) => fromJson(value)).toList();

  static StrategyAttributeWellKnownNames copyWith(
          StrategyAttributeWellKnownNames instance) =>
      instance;

  static Map<String, StrategyAttributeWellKnownNames> mapFromJson(
      Map<String, dynamic> json) {
    final map = <String, StrategyAttributeWellKnownNames>{};
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
