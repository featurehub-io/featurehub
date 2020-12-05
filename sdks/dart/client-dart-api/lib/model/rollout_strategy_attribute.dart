part of featurehub_client_api.api;

// RolloutStrategyAttribute
class RolloutStrategyAttribute {
  /* A temporary id used only when validating. Saving strips these out as they are not otherwise necessary */
  String id;

  RolloutStrategyAttributeConditional conditional;
  //enum conditionalEnum {  EQUALS,  ENDS_WITH,  STARTS_WITH,  GREATER,  GREATER_EQUALS,  LESS,  LESS_EQUALS,  NOT_EQUALS,  INCLUDES,  EXCLUDES,  REGEX,  };{

  String fieldName;
  /* the value(s) associated with this rule */
  List<dynamic> values = [];

  RolloutStrategyFieldType type;
  //enum typeEnum {  STRING,  SEMANTIC_VERSION,  NUMBER,  DATE,  DATETIME,  BOOLEAN,  IP_ADDRESS,  };{
  RolloutStrategyAttribute();

  @override
  String toString() {
    return 'RolloutStrategyAttribute[id=$id, conditional=$conditional, fieldName=$fieldName, values=$values, type=$type, ]';
  }

  fromJson(Map<String, dynamic> json) {
    if (json == null) return;

    {
      final _jsonData = json[r'id'];
      id = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
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
  }

  RolloutStrategyAttribute.fromJson(Map<String, dynamic> json) {
    fromJson(json); // allows child classes to call
  }

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (id != null) {
      json[r'id'] = LocalApiClient.serialize(id);
    }
    if (conditional != null) {
      json[r'conditional'] = LocalApiClient.serialize(conditional);
    }
    if (fieldName != null) {
      json[r'fieldName'] = LocalApiClient.serialize(fieldName);
    }
    if (values != null) {
      json[r'values'] = LocalApiClient.serialize(values);
    }
    if (type != null) {
      json[r'type'] = LocalApiClient.serialize(type);
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
      return id == other.id &&
          conditional == other.conditional &&
          fieldName == other.fieldName &&
          const ListEquality().equals(values, other.values) &&
          type == other.type;
    }

    return false;
  }

  @override
  int get hashCode {
    var hashCode = runtimeType.hashCode;

    if (id != null) {
      hashCode = hashCode ^ id.hashCode;
    }

    if (conditional != null) {
      hashCode = hashCode ^ conditional.hashCode;
    }

    if (fieldName != null) {
      hashCode = hashCode ^ fieldName.hashCode;
    }

    hashCode = hashCode ^ const ListEquality().hash(values);

    if (type != null) {
      hashCode = hashCode ^ type.hashCode;
    }

    return hashCode;
  }

  RolloutStrategyAttribute copyWith({
    String id,
    RolloutStrategyAttributeConditional conditional,
    String fieldName,
    List<dynamic> values,
    RolloutStrategyFieldType type,
  }) {
    RolloutStrategyAttribute copy = RolloutStrategyAttribute();
    copy.id = id ?? this.id;
    copy.conditional = conditional ?? this.conditional;
    copy.fieldName = fieldName ?? this.fieldName;
    {
      var newVal;
      final v = values ?? this.values;
      newVal = <dynamic>[]..addAll((v ?? []));
      copy.values = newVal;
    }
    copy.type = type ?? this.type;
    return copy;
  }
}
