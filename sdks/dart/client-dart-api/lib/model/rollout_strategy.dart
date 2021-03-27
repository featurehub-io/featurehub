part of featurehub_client_api.api;

// RolloutStrategy
class RolloutStrategy {
  String? id;
  /* names are unique in a case insensitive fashion */
  late String name;
  /* value between 0 and 1000000 - for four decimal places */
  int? percentage;
  /* if you don't wish to apply percentage based on user id, you can use one or more attributes defined here */
  List<String> percentageAttributes = [];
  /* the colour used to display the strategy in the UI. indexed table of background/foreground combos. */
  int? colouring;
  /* url to avatar (if any). Not sent to SDK. Preferably a unicorn. */
  String? avatar;
  /* when we attach the RolloutStrategy for Dacha or SSE this lets us push the value out. Only visible in SDK and SSE Edge. */
  dynamic? value;

  List<RolloutStrategyAttribute> attributes = [];
  RolloutStrategy({
    this.id,
    required this.name,
    this.percentage,
    List<String>? percentageAttributes,
    this.colouring,
    this.avatar,
    this.value,
    List<RolloutStrategyAttribute>? attributes,
  })  : this.percentageAttributes = percentageAttributes ?? [],
        this.attributes = attributes ?? [];

  @override
  String toString() {
    return 'RolloutStrategy[id=$id, name=$name, percentage=$percentage, percentageAttributes=$percentageAttributes, colouring=$colouring, avatar=$avatar, value=$value, attributes=$attributes, ]';
  }

  fromJson(Map<String, dynamic>? json) {
    if (json == null) return;

    id = (json[r'id'] == null) ? null : (json[r'id'] as String);

    {
      final _jsonData = json[r'name'];
      if (_jsonData == null)
        throw DeserialisationError(json, r'name', r'',
            'name field is null and is required to have a value');
      name = (_jsonData as String);
    }

    percentage =
        (json[r'percentage'] == null) ? null : (json[r'percentage'] as int);

    {
      final _jsonData = json[r'percentageAttributes'];
      percentageAttributes = (_jsonData == null)
          ? []
          : ((dynamic data) {
              return data?.cast<String>();
            }(_jsonData));
    } // _jsonFieldName

    colouring =
        (json[r'colouring'] == null) ? null : (json[r'colouring'] as int);

    avatar = (json[r'avatar'] == null) ? null : (json[r'avatar'] as String);

    value = (json[r'value'] == null) ? null : (json[r'value'] as dynamic);

    {
      final _jsonData = json[r'attributes'];
      attributes = (_jsonData == null)
          ? []
          : ((dynamic data) {
              return RolloutStrategyAttribute.listFromJson(data);
            }(_jsonData));
    } // _jsonFieldName
  }

  RolloutStrategy.fromJson(Map<String, dynamic>? json) {
    fromJson(json); // allows child classes to call
  }

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (id != null) {
      json[r'id'] = id;
    }
    json[r'name'] = name;
    if (percentage != null) {
      json[r'percentage'] = percentage;
    }
    if (percentageAttributes.isNotEmpty) {
      json[r'percentageAttributes'] =
          percentageAttributes.map((v) => LocalApiClient.serialize(v)).toList();
    }
    if (colouring != null) {
      json[r'colouring'] = colouring;
    }
    if (avatar != null) {
      json[r'avatar'] = avatar;
    }
    if (value != null) {
      json[r'value'] = value;
    }
    if (attributes.isNotEmpty) {
      json[r'attributes'] =
          attributes.map((v) => LocalApiClient.serialize(v)).toList();
    }
    return json;
  }

  static List<RolloutStrategy> listFromJson(List<dynamic>? json) {
    return json == null
        ? <RolloutStrategy>[]
        : json.map((value) => RolloutStrategy.fromJson(value)).toList();
  }

  static Map<String, RolloutStrategy> mapFromJson(Map<String, dynamic>? json) {
    final map = <String, RolloutStrategy>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) =>
          map[key] = RolloutStrategy.fromJson(value));
    }
    return map;
  }

  @override
  bool operator ==(Object? other) {
    if (identical(this, other)) {
      return true;
    }

    if (other is RolloutStrategy && runtimeType == other.runtimeType) {
      return id == other.id &&
          name == other.name &&
          percentage == other.percentage &&
          const ListEquality()
              .equals(percentageAttributes, other.percentageAttributes) &&
          colouring == other.colouring &&
          avatar == other.avatar &&
          value == other.value &&
          const ListEquality().equals(attributes, other.attributes);
    }

    return false;
  }

  @override
  int get hashCode {
    var hashCode = runtimeType.hashCode;

    if (id != null) {
      hashCode = hashCode * 31 + id.hashCode;
    }

    hashCode = hashCode * 31 + name.hashCode;

    if (percentage != null) {
      hashCode = hashCode * 31 + percentage.hashCode;
    }

    if (percentageAttributes.isNotEmpty) {
      hashCode =
          hashCode * 31 + const ListEquality().hash(percentageAttributes);
    }

    if (colouring != null) {
      hashCode = hashCode * 31 + colouring.hashCode;
    }

    if (avatar != null) {
      hashCode = hashCode * 31 + avatar.hashCode;
    }

    if (value != null) {
      hashCode = hashCode * 31 + value.hashCode;
    }

    if (attributes.isNotEmpty) {
      hashCode = hashCode * 31 + const ListEquality().hash(attributes);
    }

    return hashCode;
  }

  RolloutStrategy copyWith({
    String? id,
    String? name,
    int? percentage,
    List<String>? percentageAttributes,
    int? colouring,
    String? avatar,
    dynamic? value,
    List<RolloutStrategyAttribute>? attributes,
  }) {
    id ??= this.id;
    name ??= this.name;
    percentage ??= this.percentage;
    percentageAttributes ??= this.percentageAttributes;
    colouring ??= this.colouring;
    avatar ??= this.avatar;
    value ??= this.value;
    attributes ??= this.attributes;

    final _copy_id = id;
    final _copy_name = name;
    final _copy_percentage = percentage;
    final _copy_percentageAttributes = ((data) {
      return (data as List<String>).toList();
    }(percentageAttributes));

    final _copy_colouring = colouring;
    final _copy_avatar = avatar;
    final _copy_value = value;
    final _copy_attributes = ((data) {
      return (data as List<RolloutStrategyAttribute>)
          .map((data) => data.copyWith())
          .toList();
    }(attributes));

    return RolloutStrategy(
      id: _copy_id,
      name: _copy_name,
      percentage: _copy_percentage,
      percentageAttributes: _copy_percentageAttributes,
      colouring: _copy_colouring,
      avatar: _copy_avatar,
      value: _copy_value,
      attributes: _copy_attributes,
    );
  }
}
