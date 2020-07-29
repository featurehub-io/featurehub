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

    {
      final _jsonData = json[r'id'];
      id = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'features'];
      features =
          (_jsonData == null) ? null : FeatureState.listFromJson(_jsonData);
    } // _jsonFieldName
  }

  Environment.fromJson(Map<String, dynamic> json) {
    fromJson(json); // allows child classes to call
  }

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (id != null) {
      json[r'id'] = LocalApiClient.serialize(id);
    }
    if (features != null) {
      json[r'features'] = LocalApiClient.serialize(features);
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
      hashCode = hashCode ^ id.hashCode;
    }

    hashCode = hashCode ^ const ListEquality().hash(features);

    return hashCode;
  }

  Environment copyWith({
    String id,
    List<FeatureState> features,
  }) {
    Environment copy = Environment();
    copy.id = id ?? this.id;
    {
      var newVal;
      final v = features ?? this.features;
      newVal = <FeatureState>[]
        ..addAll((v ?? []).map((y) => y.copyWith()).toList());
      copy.features = newVal;
    }
    return copy;
  }
}
