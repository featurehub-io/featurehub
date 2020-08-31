part of featurehub_client_api.api;

// RolloutStrategy
class RolloutStrategy {
  String id;
  /* names are unique in a case insensitive fashion */
  String name;
  /* value between 0 and 1000000 - for four decimal places */
  int percentage;
  /* if you don't wish to apply percentage based on user id, you can use one or more attributes defined here */
  List<String> percentageAttributes = [];
  /* the colour used to display the strategy in the UI. indexed table of background/foreground combos. */
  int colouring;
  /* url to avatar (if any). Not sent to SDK. Preferably a unicorn. */
  String avatar;
  /* when we attach the RolloutStrategy for Dacha or SSE this lets us push the value out. Only visible in SDK and SSE Edge. */
  dynamic value;

  List<RolloutStrategyAttribute> attributes = [];
  RolloutStrategy();

  @override
  String toString() {
    return 'RolloutStrategy[id=$id, name=$name, percentage=$percentage, percentageAttributes=$percentageAttributes, colouring=$colouring, avatar=$avatar, value=$value, attributes=$attributes, ]';
  }

  fromJson(Map<String, dynamic> json) {
    if (json == null) return;

    {
      final _jsonData = json[r'id'];
      id = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'name'];
      name = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'percentage'];
      percentage = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'percentageAttributes'];
      percentageAttributes =
          (_jsonData == null) ? null : (_jsonData as List)?.cast<String>();
    } // _jsonFieldName
    {
      final _jsonData = json[r'colouring'];
      colouring = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'avatar'];
      avatar = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'value'];
      value = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'attributes'];
      attributes = (_jsonData == null)
          ? null
          : RolloutStrategyAttribute.listFromJson(_jsonData);
    } // _jsonFieldName
  }

  RolloutStrategy.fromJson(Map<String, dynamic> json) {
    fromJson(json); // allows child classes to call
  }

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (id != null) {
      json[r'id'] = LocalApiClient.serialize(id);
    }
    if (name != null) {
      json[r'name'] = LocalApiClient.serialize(name);
    }
    if (percentage != null) {
      json[r'percentage'] = LocalApiClient.serialize(percentage);
    }
    if (percentageAttributes != null) {
      json[r'percentageAttributes'] =
          LocalApiClient.serialize(percentageAttributes);
    }
    if (colouring != null) {
      json[r'colouring'] = LocalApiClient.serialize(colouring);
    }
    if (avatar != null) {
      json[r'avatar'] = LocalApiClient.serialize(avatar);
    }
    if (value != null) {
      json[r'value'] = LocalApiClient.serialize(value);
    }
    if (attributes != null) {
      json[r'attributes'] = LocalApiClient.serialize(attributes);
    }
    return json;
  }

  static List<RolloutStrategy> listFromJson(List<dynamic> json) {
    return json == null
        ? <RolloutStrategy>[]
        : json.map((value) => RolloutStrategy.fromJson(value)).toList();
  }

  static Map<String, RolloutStrategy> mapFromJson(Map<String, dynamic> json) {
    final map = <String, RolloutStrategy>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) =>
          map[key] = RolloutStrategy.fromJson(value));
    }
    return map;
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }

    if (other is RolloutStrategy && runtimeType == other.runtimeType) {
      return id == other.id &&
          name == other.name &&
          percentage == other.percentage &&
          percentageAttributes == other.percentageAttributes &&
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
      hashCode = hashCode ^ id.hashCode;
    }

    if (name != null) {
      hashCode = hashCode ^ name.hashCode;
    }

    if (percentage != null) {
      hashCode = hashCode ^ percentage.hashCode;
    }

    if (percentageAttributes != null) {
      hashCode = hashCode ^ percentageAttributes.hashCode;
    }

    if (colouring != null) {
      hashCode = hashCode ^ colouring.hashCode;
    }

    if (avatar != null) {
      hashCode = hashCode ^ avatar.hashCode;
    }

    if (value != null) {
      hashCode = hashCode ^ value.hashCode;
    }

    hashCode = hashCode ^ const ListEquality().hash(attributes);

    return hashCode;
  }

  RolloutStrategy copyWith({
    String id,
    String name,
    int percentage,
    List<String> percentageAttributes,
    int colouring,
    String avatar,
    dynamic value,
    List<RolloutStrategyAttribute> attributes,
  }) {
    RolloutStrategy copy = RolloutStrategy();
    copy.id = id ?? this.id;
    copy.name = name ?? this.name;
    copy.percentage = percentage ?? this.percentage;
    copy.percentageAttributes = percentageAttributes ?? []
      ..addAll(this.percentageAttributes);
    copy.colouring = colouring ?? this.colouring;
    copy.avatar = avatar ?? this.avatar;
    copy.value = value ?? this.value;
    {
      var newVal;
      final v = attributes ?? this.attributes;
      newVal = <RolloutStrategyAttribute>[]
        ..addAll((v ?? []).map((y) => y.copyWith()).toList());
      copy.attributes = newVal;
    }
    return copy;
  }
}
