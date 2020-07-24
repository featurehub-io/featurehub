part of featurehub_client_api.api;

// StrategyPair
class StrategyPair {
  String name;

  String value;
  StrategyPair();

  @override
  String toString() {
    return 'StrategyPair[name=$name, value=$value, ]';
  }

  fromJson(Map<String, dynamic> json) {
    if (json == null) return;

    {
      final _jsonData = json[r'name'];
      name = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'value'];
      value = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
  }

  StrategyPair.fromJson(Map<String, dynamic> json) {
    fromJson(json); // allows child classes to call
  }

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (name != null) {
      json[r'name'] = LocalApiClient.serialize(name);
    }
    if (value != null) {
      json[r'value'] = LocalApiClient.serialize(value);
    }
    return json;
  }

  static List<StrategyPair> listFromJson(List<dynamic> json) {
    return json == null
        ? <StrategyPair>[]
        : json.map((value) => StrategyPair.fromJson(value)).toList();
  }

  static Map<String, StrategyPair> mapFromJson(Map<String, dynamic> json) {
    final map = <String, StrategyPair>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) =>
          map[key] = StrategyPair.fromJson(value));
    }
    return map;
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }

    if (other is StrategyPair && runtimeType == other.runtimeType) {
      return name == other.name && value == other.value;
    }

    return false;
  }

  @override
  int get hashCode {
    var hashCode = runtimeType.hashCode;

    if (name != null) {
      hashCode = hashCode ^ name.hashCode;
    }

    if (value != null) {
      hashCode = hashCode ^ value.hashCode;
    }

    return hashCode;
  }

  StrategyPair copyWith({
    String name,
    String value,
  }) {
    StrategyPair copy = StrategyPair();
    copy.name = name ?? this.name;
    copy.value = value ?? this.value;
    return copy;
  }
}
