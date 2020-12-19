part of featurehub_client_api.api;

enum StrategyAttributeWellKnownNames {
  device,
  country,
  platform,
  userkey,
  session,
  version
}

class StrategyAttributeWellKnownNamesTypeTransformer {
  static Map<String, StrategyAttributeWellKnownNames> fromJsonMap = {
    'device': StrategyAttributeWellKnownNames.device,
    'country': StrategyAttributeWellKnownNames.country,
    'platform': StrategyAttributeWellKnownNames.platform,
    'userkey': StrategyAttributeWellKnownNames.userkey,
    'session': StrategyAttributeWellKnownNames.session,
    'version': StrategyAttributeWellKnownNames.version
  };
  static Map<StrategyAttributeWellKnownNames, String> toJsonMap = {
    StrategyAttributeWellKnownNames.device: 'device',
    StrategyAttributeWellKnownNames.country: 'country',
    StrategyAttributeWellKnownNames.platform: 'platform',
    StrategyAttributeWellKnownNames.userkey: 'userkey',
    StrategyAttributeWellKnownNames.session: 'session',
    StrategyAttributeWellKnownNames.version: 'version'
  };

  static StrategyAttributeWellKnownNames fromJson(dynamic data) {
    var found = fromJsonMap[data];
    if (found == null) {
      throw ('Unknown enum value to decode: $data');
    }
    return found;
  }

  static dynamic toJson(StrategyAttributeWellKnownNames data) {
    return toJsonMap[data];
  }

  static List<StrategyAttributeWellKnownNames> listFromJson(
      List<dynamic> json) {
    return json == null
        ? <StrategyAttributeWellKnownNames>[]
        : json.map((value) => fromJson(value)).toList();
  }

  static StrategyAttributeWellKnownNames copyWith(
      StrategyAttributeWellKnownNames instance) {
    return instance;
  }

  static Map<String, StrategyAttributeWellKnownNames> mapFromJson(
      Map<String, dynamic> json) {
    final map = <String, StrategyAttributeWellKnownNames>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) => map[key] = fromJson(value));
    }
    return map;
  }
}
