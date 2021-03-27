part of featurehub_client_api.api;

// Environment
class Environment {
  String? id;

  List<FeatureState> features = [];
  Environment({
    this.id,
    List<FeatureState>? features,
  }) : this.features = features ?? [];

  @override
  String toString() {
    return 'Environment[id=$id, features=$features, ]';
  }

  fromJson(Map<String, dynamic>? json) {
    if (json == null) return;

    id = (json[r'id'] == null) ? null : (json[r'id'] as String);

    {
      final _jsonData = json[r'features'];
      features = (_jsonData == null)
          ? []
          : ((dynamic data) {
              return FeatureState.listFromJson(data);
            }(_jsonData));
    } // _jsonFieldName
  }

  Environment.fromJson(Map<String, dynamic>? json) {
    fromJson(json); // allows child classes to call
  }

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (id != null) {
      json[r'id'] = id;
    }
    if (features.isNotEmpty) {
      json[r'features'] =
          features.map((v) => LocalApiClient.serialize(v)).toList();
    }
    return json;
  }

  static List<Environment> listFromJson(List<dynamic>? json) {
    return json == null
        ? <Environment>[]
        : json.map((value) => Environment.fromJson(value)).toList();
  }

  static Map<String, Environment> mapFromJson(Map<String, dynamic>? json) {
    final map = <String, Environment>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) =>
          map[key] = Environment.fromJson(value));
    }
    return map;
  }

  @override
  bool operator ==(Object? other) {
    if (identical(this, other)) {
      return true;
    }

    if (other is Environment && runtimeType == other.runtimeType) {
      return id == other.id &&
          const ListEquality().equals(features, other.features);
    }

    return false;
  }

  @override
  int get hashCode {
    var hashCode = runtimeType.hashCode;

    if (id != null) {
      hashCode = hashCode * 31 + id.hashCode;
    }

    if (features.isNotEmpty) {
      hashCode = hashCode * 31 + const ListEquality().hash(features);
    }

    return hashCode;
  }

  Environment copyWith({
    String? id,
    List<FeatureState>? features,
  }) {
    id ??= this.id;
    features ??= this.features;

    final _copy_id = id;
    final _copy_features = ((data) {
      return (data as List<FeatureState>)
          .map((data) => data.copyWith())
          .toList();
    }(features));

    return Environment(
      id: _copy_id,
      features: _copy_features,
    );
  }
}
