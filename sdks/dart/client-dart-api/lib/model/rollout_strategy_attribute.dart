part of featurehub_client_api.api;

// RolloutStrategyAttribute
class RolloutStrategyAttribute {
  RolloutStrategyAttributeConditional conditional;
  //enum conditionalEnum {  EQUALS,  ENDS_WITH,  STARTS_WITH,  GREATER,  GREATER_EQUALS,  LESS,  LESS_EQUALS,  NOT_EQUALS,  INCLUDES,  EXCLUDES,  REGEX,  };{

  String fieldName;
  /* its value */
  dynamic value;
  /* the values if it is an array */
  List<dynamic> values = [];

  RolloutStrategyFieldType type;
  //enum typeEnum {  STRING,  SEMANTIC_VERSION,  NUMBER,  DATE,  DATETIME,  BOOLEAN,  IP_ADDRESS,  };{

  bool array;
  RolloutStrategyAttribute();

  @override
  String toString() {
    return 'RolloutStrategyAttribute[conditional=$conditional, fieldName=$fieldName, value=$value, values=$values, type=$type, array=$array, ]';
  }

  fromJson(Map<String, dynamic> json) {
    if (json == null) return;

    {
      final _jsonData = json[r'conditional'];
      conditional = (_jsonData == null)
          ? null
          : RolloutStrategyAttributeConditionalTypeTransformer.fromJson(
              _jsonData);
    } // _jsonFieldName
    {
      final _jsonData = json[r'fieldName'];
      fieldName = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'value'];
      value = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'values'];
      values =
          (_jsonData == null) ? null : (_jsonData as List)?.cast<dynamic>();
    } // _jsonFieldName
    {
      final _jsonData = json[r'type'];
      type = (_jsonData == null)
          ? null
          : RolloutStrategyFieldTypeTypeTransformer.fromJson(_jsonData);
    } // _jsonFieldName
    {
      final _jsonData = json[r'array'];
      array = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
  }

  RolloutStrategyAttribute.fromJson(Map<String, dynamic> json) {
    fromJson(json); // allows child classes to call
  }

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (conditional != null) {
      json[r'conditional'] = LocalApiClient.serialize(conditional);
    }
    if (fieldName != null) {
      json[r'fieldName'] = LocalApiClient.serialize(fieldName);
    }
    if (value != null) {
      json[r'value'] = LocalApiClient.serialize(value);
    }
    if (values != null) {
      json[r'values'] = LocalApiClient.serialize(values);
    }
    if (type != null) {
      json[r'type'] = LocalApiClient.serialize(type);
    }
    if (array != null) {
      json[r'array'] = LocalApiClient.serialize(array);
    }
    return json;
  }

  static List<RolloutStrategyAttribute> listFromJson(List<dynamic> json) {
    return json == null
        ? <RolloutStrategyAttribute>[]
        : json
            .map((value) => RolloutStrategyAttribute.fromJson(value))
            .toList();
  }

  static Map<String, RolloutStrategyAttribute> mapFromJson(
      Map<String, dynamic> json) {
    final map = <String, RolloutStrategyAttribute>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) =>
          map[key] = RolloutStrategyAttribute.fromJson(value));
    }
    return map;
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }

    if (other is RolloutStrategyAttribute && runtimeType == other.runtimeType) {
      return conditional == other.conditional &&
          fieldName == other.fieldName &&
          value == other.value &&
          const ListEquality().equals(values, other.values) &&
          type == other.type &&
          array == other.array;
    }

    return false;
  }

  @override
  int get hashCode {
    var hashCode = runtimeType.hashCode;

    if (conditional != null) {
      hashCode = hashCode ^ conditional.hashCode;
    }

    if (fieldName != null) {
      hashCode = hashCode ^ fieldName.hashCode;
    }

    if (value != null) {
      hashCode = hashCode ^ value.hashCode;
    }

    hashCode = hashCode ^ const ListEquality().hash(values);

    if (type != null) {
      hashCode = hashCode ^ type.hashCode;
    }

    if (array != null) {
      hashCode = hashCode ^ array.hashCode;
    }

    return hashCode;
  }

  RolloutStrategyAttribute copyWith({
    RolloutStrategyAttributeConditional conditional,
    String fieldName,
    dynamic value,
    List<dynamic> values,
    RolloutStrategyFieldType type,
    bool array,
  }) {
    RolloutStrategyAttribute copy = RolloutStrategyAttribute();
    copy.conditional = conditional ?? this.conditional;
    copy.fieldName = fieldName ?? this.fieldName;
    copy.value = value ?? this.value;
    {
      var newVal;
      final v = values ?? this.values;
      newVal = <dynamic>[]..addAll((v ?? []));
      copy.values = newVal;
    }
    copy.type = type ?? this.type;
    copy.array = array ?? this.array;
    return copy;
  }
}
