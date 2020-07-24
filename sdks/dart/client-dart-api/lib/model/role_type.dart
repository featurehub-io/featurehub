part of featurehub_client_api.api;

enum RoleType { READ, LOCK, UNLOCK, CHANGE_VALUE }

class RoleTypeTypeTransformer {
  static Map<String, RoleType> fromJsonMap = {
    'READ': RoleType.READ,
    'LOCK': RoleType.LOCK,
    'UNLOCK': RoleType.UNLOCK,
    'CHANGE_VALUE': RoleType.CHANGE_VALUE
  };
  static Map<RoleType, String> toJsonMap = {
    RoleType.READ: 'READ',
    RoleType.LOCK: 'LOCK',
    RoleType.UNLOCK: 'UNLOCK',
    RoleType.CHANGE_VALUE: 'CHANGE_VALUE'
  };

  static RoleType fromJson(dynamic data) {
    var found = fromJsonMap[data];
    if (found == null) {
      throw ('Unknown enum value to decode: $data');
    }
    return found;
  }

  static dynamic toJson(RoleType data) {
    return toJsonMap[data];
  }

  static List<RoleType> listFromJson(List<dynamic> json) {
    return json == null
        ? <RoleType>[]
        : json.map((value) => fromJson(value)).toList();
  }

  static RoleType copyWith(RoleType instance) {
    return instance;
  }

  static Map<String, RoleType> mapFromJson(Map<String, dynamic> json) {
    final map = <String, RoleType>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) => map[key] = fromJson(value));
    }
    return map;
  }
}
