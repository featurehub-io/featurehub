part of featurehub_client_api.api;

// RolloutStrategyAttribute
class RolloutStrategyAttribute {
  /* A temporary id used only when validating. Saving strips these out as they are not otherwise necessary */
  String id;

  RolloutStrategyAttributeConditional conditional;

  String fieldName;
  /* the value(s) associated with this rule */
  List<dynamic> values = [];

  RolloutStrategyFieldType type;
  RolloutStrategyAttribute({
    this.id,
    this.conditional,
    this.fieldName,
    this.values,
    this.type,
  });

  @override
  String toString() {
    return 'RolloutStrategyAttribute[id=$id, conditional=$conditional, fieldName=$fieldName, values=$values, type=$type, ]';
  }

  fromJson(Map<String, dynamic> json) {
    if (json == null) return;

    id = (json[r'id'] == null)
        ? null
        : id = (json[r'id'] == null) ? null : (json[r'id'] as String);
    ;
    conditional = (json[r'conditional'] == null)
        ? null
        : conditional = (json[r'conditional'] == null)
            ? null
            : RolloutStrategyAttributeConditionalExtension.fromJson(
                json[r'conditional']);
    ;
    fieldName = (json[r'fieldName'] == null)
        ? null
        : fieldName = (json[r'fieldName'] == null)
            ? null
            : (json[r'fieldName'] as String);
    ;
    {
      final _jsonData = json[r'values'];
      values = (_jsonData == null)
          ? []
          : ((dynamic data) {
              return data?.cast<dynamic>();
            }(_jsonData));
    } // _jsonFieldName

    type = (json[r'type'] == null)
        ? null
        : type = (json[r'type'] == null)
            ? null
            : RolloutStrategyFieldTypeExtension.fromJson(json[r'type']);
    ;
  }

  RolloutStrategyAttribute.fromJson(Map<String, dynamic> json) {
    fromJson(json); // allows child classes to call
  }

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (id != null) {
      json[r'id'] = id;
    }
    if (conditional != null) {
      json[r'conditional'] = conditional?.toJson();
    }
    if (fieldName != null) {
      json[r'fieldName'] = fieldName;
    }
    if (values != null) {
      json[r'values'] =
          values?.map((v) => LocalApiClient.serialize(v))?.toList();
    }
    if (type != null) {
      json[r'type'] = type?.toJson();
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
          conditional == other.conditional && // other

          fieldName == other.fieldName &&
          const ListEquality().equals(values, other.values) &&
          type == other.type;
    }

    return false;
  }

  @override
  int get hashCode {
    var hashCode = runtimeType.hashCode;

    hashCode = hashCode * 31 + id.hashCode;

    hashCode = hashCode * 31 + conditional.hashCode;

    hashCode = hashCode * 31 + fieldName.hashCode;

    hashCode = hashCode * 31 + const ListEquality().hash(values);

    hashCode = hashCode * 31 + type.hashCode;

    return hashCode;
  }

  RolloutStrategyAttribute copyWith({
    String id,
    RolloutStrategyAttributeConditional conditional,
    String fieldName,
    List<dynamic> values,
    RolloutStrategyFieldType type,
  }) {
    id ??= this.id;
    conditional ??= this.conditional;
    fieldName ??= this.fieldName;
    values ??= this.values;
    type ??= this.type;

    final _copy_id = id;
    final _copy_conditional = conditional;
    final _copy_fieldName = fieldName;
    final _copy_values = (values == null)
        ? [] as List<dynamic>
        : ((data) {
            return (data as List<dynamic>)?.toList();
          }(values));

    final _copy_type = type;

    return RolloutStrategyAttribute(
      id: _copy_id,
      conditional: _copy_conditional,
      fieldName: _copy_fieldName,
      values: _copy_values,
      type: _copy_type,
    );
  }
}
