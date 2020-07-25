part of featurehub_client_api.api;

// FeatureStateUpdate
class FeatureStateUpdate {
  /* the new value */
  dynamic value;
  /* indicates whether you are trying to update the value, as value can be null */
  bool updateValue;
  /* set only if you wish to lock or unlock, otherwise null */
  bool lock;
  FeatureStateUpdate();

  @override
  String toString() {
    return 'FeatureStateUpdate[value=$value, updateValue=$updateValue, lock=$lock, ]';
  }

  fromJson(Map<String, dynamic> json) {
    if (json == null) return;

    {
      final _jsonData = json[r'value'];
      value = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'updateValue'];
      updateValue = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'lock'];
      lock = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
  }

  FeatureStateUpdate.fromJson(Map<String, dynamic> json) {
    fromJson(json); // allows child classes to call
  }

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (value != null) {
      json[r'value'] = LocalApiClient.serialize(value);
    }
    if (updateValue != null) {
      json[r'updateValue'] = LocalApiClient.serialize(updateValue);
    }
    if (lock != null) {
      json[r'lock'] = LocalApiClient.serialize(lock);
    }
    return json;
  }

  static List<FeatureStateUpdate> listFromJson(List<dynamic> json) {
    return json == null
        ? <FeatureStateUpdate>[]
        : json.map((value) => FeatureStateUpdate.fromJson(value)).toList();
  }

  static Map<String, FeatureStateUpdate> mapFromJson(
      Map<String, dynamic> json) {
    final map = <String, FeatureStateUpdate>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) =>
          map[key] = FeatureStateUpdate.fromJson(value));
    }
    return map;
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }

    if (other is FeatureStateUpdate && runtimeType == other.runtimeType) {
      return value == other.value &&
          updateValue == other.updateValue &&
          lock == other.lock;
    }

    return false;
  }

  @override
  int get hashCode {
    var hashCode = runtimeType.hashCode;

    if (value != null) {
      hashCode = hashCode ^ value.hashCode;
    }

    if (updateValue != null) {
      hashCode = hashCode ^ updateValue.hashCode;
    }

    if (lock != null) {
      hashCode = hashCode ^ lock.hashCode;
    }

    return hashCode;
  }

  FeatureStateUpdate copyWith({
    dynamic value,
    bool updateValue,
    bool lock,
  }) {
    FeatureStateUpdate copy = FeatureStateUpdate();
    copy.value = value ?? this.value;
    copy.updateValue = updateValue ?? this.updateValue;
    copy.lock = lock ?? this.lock;
    return copy;
  }
}
