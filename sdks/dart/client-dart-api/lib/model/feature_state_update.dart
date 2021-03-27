part of featurehub_client_api.api;

// FeatureStateUpdate
class FeatureStateUpdate {
  /* the new value */
  dynamic value;
  /* indicates whether you are trying to update the value, as value can be null */
  bool updateValue;
  /* set only if you wish to lock or unlock, otherwise null */
  bool lock;
  FeatureStateUpdate({
    this.value,
    this.updateValue,
    this.lock,
  });

  @override
  String toString() {
    return 'FeatureStateUpdate[value=$value, updateValue=$updateValue, lock=$lock, ]';
  }

  fromJson(Map<String, dynamic> json) {
    if (json == null) return;

    value = (json[r'value'] == null)
        ? null
        : value = (json[r'value'] == null) ? null : (json[r'value'] as dynamic);
    ;
    updateValue = (json[r'updateValue'] == null)
        ? null
        : updateValue = (json[r'updateValue'] == null)
            ? null
            : (json[r'updateValue'] as bool);
    ;
    lock = (json[r'lock'] == null)
        ? null
        : lock = (json[r'lock'] == null) ? null : (json[r'lock'] as bool);
    ;
  }

  FeatureStateUpdate.fromJson(Map<String, dynamic> json) {
    fromJson(json); // allows child classes to call
  }

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (value != null) {
      json[r'value'] = value;
    }
    if (updateValue != null) {
      json[r'updateValue'] = updateValue;
    }
    if (lock != null) {
      json[r'lock'] = lock;
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

    hashCode = hashCode * 31 + value.hashCode;

    hashCode = hashCode * 31 + updateValue.hashCode;

    hashCode = hashCode * 31 + lock.hashCode;

    return hashCode;
  }

  FeatureStateUpdate copyWith({
    dynamic value,
    bool updateValue,
    bool lock,
  }) {
    value ??= this.value;
    updateValue ??= this.updateValue;
    lock ??= this.lock;

    final _copy_value = value;
    final _copy_updateValue = updateValue;
    final _copy_lock = lock;

    return FeatureStateUpdate(
      value: _copy_value,
      updateValue: _copy_updateValue,
      lock: _copy_lock,
    );
  }
}
