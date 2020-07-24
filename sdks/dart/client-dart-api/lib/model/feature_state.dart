part of featurehub_client_api.api;

// FeatureState
class FeatureState {
  String id;

  String key;

  int version;

  FeatureValueType type;
  //enum typeEnum {  BOOLEAN,  STRING,  NUMBER,  JSON,  };{
  /* the current value */
  dynamic value;

  Strategy strategy;
  FeatureState();

  @override
  String toString() {
    return 'FeatureState[id=$id, key=$key, version=$version, type=$type, value=$value, strategy=$strategy, ]';
  }

  fromJson(Map<String, dynamic> json) {
    if (json == null) return;

    {
      final _jsonData = json[r'id'];
      id = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'key'];
      key = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'version'];
      version = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'type'];
      type = (_jsonData == null)
          ? null
          : FeatureValueTypeTypeTransformer.fromJson(_jsonData);
    } // _jsonFieldName
    {
      final _jsonData = json[r'value'];
      value = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'strategy'];
      strategy = (_jsonData == null) ? null : Strategy.fromJson(_jsonData);
    } // _jsonFieldName
  }

  FeatureState.fromJson(Map<String, dynamic> json) {
    fromJson(json); // allows child classes to call
  }

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (id != null) {
      json[r'id'] = LocalApiClient.serialize(id);
    }
    if (key != null) {
      json[r'key'] = LocalApiClient.serialize(key);
    }
    if (version != null) {
      json[r'version'] = LocalApiClient.serialize(version);
    }
    if (type != null) {
      json[r'type'] = LocalApiClient.serialize(type);
    }
    if (value != null) {
      json[r'value'] = LocalApiClient.serialize(value);
    }
    if (strategy != null) {
      json[r'strategy'] = LocalApiClient.serialize(strategy);
    }
    return json;
  }

  static List<FeatureState> listFromJson(List<dynamic> json) {
    return json == null
        ? <FeatureState>[]
        : json.map((value) => FeatureState.fromJson(value)).toList();
  }

  static Map<String, FeatureState> mapFromJson(Map<String, dynamic> json) {
    final map = <String, FeatureState>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) =>
          map[key] = FeatureState.fromJson(value));
    }
    return map;
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }

    if (other is FeatureState && runtimeType == other.runtimeType) {
      return id == other.id &&
          key == other.key &&
          version == other.version &&
          type == other.type &&
          value == other.value &&
          strategy == other.strategy;
    }

    return false;
  }

  @override
  int get hashCode {
    var hashCode = runtimeType.hashCode;

    if (id != null) {
      hashCode = hashCode ^ id.hashCode;
    }

    if (key != null) {
      hashCode = hashCode ^ key.hashCode;
    }

    if (version != null) {
      hashCode = hashCode ^ version.hashCode;
    }

    if (type != null) {
      hashCode = hashCode ^ type.hashCode;
    }

    if (value != null) {
      hashCode = hashCode ^ value.hashCode;
    }

    if (strategy != null) {
      hashCode = hashCode ^ strategy.hashCode;
    }

    return hashCode;
  }

  FeatureState copyWith({
    String id,
    String key,
    int version,
    FeatureValueType type,
    dynamic value,
    Strategy strategy,
  }) {
    FeatureState copy = FeatureState();
    copy.id = id ?? this.id;
    copy.key = key ?? this.key;
    copy.version = version ?? this.version;
    copy.type = type ?? this.type;
    copy.value = value ?? this.value;
    copy.strategy = strategy ?? this.strategy?.copyWith();
    return copy;
  }
}
