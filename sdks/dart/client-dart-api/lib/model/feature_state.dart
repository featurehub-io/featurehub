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
  /* the current value */
  dynamic value;
  /* This field is filled in from the client side in the GET api as the GET api is able to request multiple environments. It is never passed from the server, as an array of feature states is wrapped in an environment. */
  String environmentId;

  List<RolloutStrategy> strategies = [];
  FeatureState({
    this.id,
    this.key,
    this.l,
    this.version,
    this.type,
    this.value,
    this.environmentId,
    this.strategies,
  });

  @override
  String toString() {
    return 'FeatureState[id=$id, key=$key, l=$l, version=$version, type=$type, value=$value, environmentId=$environmentId, strategies=$strategies, ]';
  }

  fromJson(Map<String, dynamic> json) {
    if (json == null) return;

    id = (json[r'id'] == null)
        ? null
        : id = (json[r'id'] == null) ? null : (json[r'id'] as String);
    ;
    key = (json[r'key'] == null)
        ? null
        : key = (json[r'key'] == null) ? null : (json[r'key'] as String);
    ;
    l = (json[r'l'] == null)
        ? null
        : l = (json[r'l'] == null) ? null : (json[r'l'] as bool);
    ;
    version = (json[r'version'] == null)
        ? null
        : version =
            (json[r'version'] == null) ? null : (json[r'version'] as int);
    ;
    type = (json[r'type'] == null)
        ? null
        : type = (json[r'type'] == null)
            ? null
            : FeatureValueTypeExtension.fromJson(json[r'type']);
    ;
    value = (json[r'value'] == null)
        ? null
        : value = (json[r'value'] == null) ? null : (json[r'value'] as dynamic);
    ;
    environmentId = (json[r'environmentId'] == null)
        ? null
        : environmentId = (json[r'environmentId'] == null)
            ? null
            : (json[r'environmentId'] as String);
    ;
    {
      final _jsonData = json[r'strategies'];
      strategies = (_jsonData == null)
          ? []
          : ((dynamic data) {
              return RolloutStrategy.listFromJson(data);
            }(_jsonData));
    } // _jsonFieldName
  }

  FeatureState.fromJson(Map<String, dynamic> json) {
    fromJson(json); // allows child classes to call
  }

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (id != null) {
      json[r'id'] = id;
    }
    if (key != null) {
      json[r'key'] = key;
    }
    if (l != null) {
      json[r'l'] = l;
    }
    if (version != null) {
      json[r'version'] = version;
    }
    if (type != null) {
      json[r'type'] = type?.toJson();
    }
    if (value != null) {
      json[r'value'] = value;
    }
    if (environmentId != null) {
      json[r'environmentId'] = environmentId;
    }
    if (strategies != null) {
      json[r'strategies'] =
          strategies?.map((v) => LocalApiClient.serialize(v))?.toList();
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
          type == other.type && // other

          value == other.value &&
          environmentId == other.environmentId &&
          const ListEquality().equals(strategies, other.strategies);
    }

    return false;
  }

  @override
  int get hashCode {
    var hashCode = runtimeType.hashCode;

    hashCode = hashCode * 31 + id.hashCode;

    hashCode = hashCode * 31 + key.hashCode;

    hashCode = hashCode * 31 + l.hashCode;

    hashCode = hashCode * 31 + version.hashCode;

    hashCode = hashCode * 31 + type.hashCode;

    hashCode = hashCode * 31 + value.hashCode;

    hashCode = hashCode * 31 + environmentId.hashCode;

    hashCode = hashCode * 31 + const ListEquality().hash(strategies);

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
    id ??= this.id;
    key ??= this.key;
    l ??= this.l;
    version ??= this.version;
    type ??= this.type;
    value ??= this.value;
    environmentId ??= this.environmentId;
    strategies ??= this.strategies;

    final _copy_id = id;
    final _copy_key = key;
    final _copy_l = l;
    final _copy_version = version;
    final _copy_type = type;
    final _copy_value = value;
    final _copy_environmentId = environmentId;
    final _copy_strategies = (strategies == null)
        ? [] as List<RolloutStrategy>
        : ((data) {
            return (data as List<RolloutStrategy>)
                ?.map((data) => data.copyWith())
                .toList();
          }(strategies));

    return FeatureState(
      id: _copy_id,
      key: _copy_key,
      l: _copy_l,
      version: _copy_version,
      type: _copy_type,
      value: _copy_value,
      environmentId: _copy_environmentId,
      strategies: _copy_strategies,
    );
  }
}
