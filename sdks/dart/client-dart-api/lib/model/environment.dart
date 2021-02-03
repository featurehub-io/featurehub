part of featurehub_client_api.api;

// Environment
class Environment {
  String id;

  List<FeatureState> features = [];
  Environment();

  @override
  String toString() {
    return 'Environment[id=$id, features=$features, ]';
  }

  fromJson(Map<String, dynamic> json) {
    if (json == null) return;

    id = (json[r'id'] == null) ? null : (json[r'id'] as String);
    {
      final _jsonData = json[r'features'];
      features = (_jsonData == null)
          ? null
          : ((dynamic data) {
              return FeatureState.listFromJson(data);
            }(_jsonData));
    } // _jsonFieldName
  }

  Environment.fromJson(Map<String, dynamic> json) {
    fromJson(json); // allows child classes to call
  }

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (id != null) {
      json[r'id'] = id;
    }
    if (features != null) {
      json[r'features'] =
          features.map((v) => LocalApiClient.serialize(v)).toList();
    }
    return json;
  }

  static List<Environment> listFromJson(List<dynamic> json) {
    return json == null
        ? <Environment>[]
        : json.map((value) => Environment.fromJson(value)).toList();
  }

  static Map<String, Environment> mapFromJson(Map<String, dynamic> json) {
    final map = <String, Environment>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) =>
          map[key] = Environment.fromJson(value));
    }
    return map;
  }

  @override
  bool operator ==(Object other) {
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

    if (features != null) {
      hashCode = hashCode * 31 + const ListEquality().hash(features);
    }

    return hashCode;
  }

  Environment copyWith({
    String id,
    List<FeatureState> features,
  }) {
    Environment copy = Environment();
    id ??= this.id;
    features ??= this.features;

    copy.id = id;
    copy.features = (features == null)
        ? null
        : ((data) {
            return (data as List<FeatureState>)
                .map((data) => data.copyWith())
                .toList();
          }(features));

    return copy;
  }
}
