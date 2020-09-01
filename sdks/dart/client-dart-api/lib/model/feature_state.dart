part of featurehub_client_api.api;

// FeatureState
class FeatureState {
  String id;

  String key;
  /* Is this feature locked. Usually this doesn't matter because the value is the value, but for FeatureInterceptors it can matter. */
  bool l;
  /* The version of the feature, this allows features to change values and it means we don't trigger events */
  int version;

  FeatureValueType type;
  //enum typeEnum {  BOOLEAN,  STRING,  NUMBER,  JSON,  };{
  /* the current value */
  dynamic value;
  /* This field is filled in from the client side in the GET api as the GET api is able to request multiple environments. It is never passed from the server, as an array of feature states is wrapped in an environment. */
  String environmentId;

  List<RolloutStrategy> strategies = [];
  FeatureState();

  @override
  String toString() {
    return 'FeatureState[id=$id, key=$key, l=$l, version=$version, type=$type, value=$value, environmentId=$environmentId, strategies=$strategies, ]';
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
      final _jsonData = json[r'l'];
      l = (_jsonData == null) ? null : _jsonData;
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
      final _jsonData = json[r'environmentId'];
      environmentId = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'strategies'];
      strategies =
          (_jsonData == null) ? null : RolloutStrategy.listFromJson(_jsonData);
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
    if (l != null) {
      json[r'l'] = LocalApiClient.serialize(l);
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
    if (environmentId != null) {
      json[r'environmentId'] = LocalApiClient.serialize(environmentId);
    }
    if (strategies != null) {
      json[r'strategies'] = LocalApiClient.serialize(strategies);
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
          l == other.l &&
          version == other.version &&
          type == other.type &&
          value == other.value &&
          environmentId == other.environmentId &&
          const ListEquality().equals(strategies, other.strategies);
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

    if (l != null) {
      hashCode = hashCode ^ l.hashCode;
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

    if (environmentId != null) {
      hashCode = hashCode ^ environmentId.hashCode;
    }

    hashCode = hashCode ^ const ListEquality().hash(strategies);

    return hashCode;
  }

  FeatureState copyWith({
    String id,
    String key,
    bool l,
    int version,
    FeatureValueType type,
    dynamic value,
    String environmentId,
    List<RolloutStrategy> strategies,
  }) {
    FeatureState copy = FeatureState();
    copy.id = id ?? this.id;
    copy.key = key ?? this.key;
    copy.l = l ?? this.l;
    copy.version = version ?? this.version;
    copy.type = type ?? this.type;
    copy.value = value ?? this.value;
    copy.environmentId = environmentId ?? this.environmentId;
    {
      var newVal;
      final v = strategies ?? this.strategies;
      newVal = <RolloutStrategy>[]
        ..addAll((v ?? []).map((y) => y.copyWith()).toList());
      copy.strategies = newVal;
    }
    return copy;
  }
}
