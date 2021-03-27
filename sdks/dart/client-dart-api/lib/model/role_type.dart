part of featurehub_client_api.api;

enum RoleType { READ, LOCK, UNLOCK, CHANGE_VALUE }

extension RoleTypeExtension on RoleType {
  String get name => toMap[this];

  // you have to call this extension class to use this as this is not yet supported
  static RoleType type(String name) => fromMap[name];

  static Map<String, RoleType> fromMap = {
    'READ': RoleType.READ,
    'LOCK': RoleType.LOCK,
    'UNLOCK': RoleType.UNLOCK,
    'CHANGE_VALUE': RoleType.CHANGE_VALUE
  };
  static Map<RoleType, String> toMap = {
    RoleType.READ: 'READ',
    RoleType.LOCK: 'LOCK',
    RoleType.UNLOCK: 'UNLOCK',
    RoleType.CHANGE_VALUE: 'CHANGE_VALUE'
  };

  static RoleType fromJson(dynamic data) => data == null ? null : fromMap[data];

  dynamic toJson() => toMap[this];

  static List<RoleType> listFromJson(List<dynamic> json) => json == null
      ? <RoleType>[]
      : json.map((value) => fromJson(value)).toList();

  static RoleType copyWith(RoleType instance) => instance;

  static Map<String, RoleType> mapFromJson(Map<String, dynamic> json) {
    final map = <String, RoleType>{};
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
