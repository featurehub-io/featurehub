part of featurehub_client_api.api;

// Strategy
class Strategy {
  StrategyNameType name;
  //enum nameEnum {  ATTRIBUTE,  PERCENTAGE,  };{
  /* this value is used if it is a simple attribute or percentage. If it is more complex then the pairs are passed */
  dynamic value;

  List<StrategyPair> pairs = [];
  Strategy();

  @override
  String toString() {
    return 'Strategy[name=$name, value=$value, pairs=$pairs, ]';
  }

  fromJson(Map<String, dynamic> json) {
    if (json == null) return;

    {
      final _jsonData = json[r'name'];
      name = (_jsonData == null)
          ? null
          : StrategyNameTypeTypeTransformer.fromJson(_jsonData);
    } // _jsonFieldName
    {
      final _jsonData = json[r'value'];
      value = (_jsonData == null) ? null : _jsonData;
    } // _jsonFieldName
    {
      final _jsonData = json[r'pairs'];
      pairs = (_jsonData == null) ? null : StrategyPair.listFromJson(_jsonData);
    } // _jsonFieldName
  }

  Strategy.fromJson(Map<String, dynamic> json) {
    fromJson(json); // allows child classes to call
  }

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (name != null) {
      json[r'name'] = LocalApiClient.serialize(name);
    }
    if (value != null) {
      json[r'value'] = LocalApiClient.serialize(value);
    }
    if (pairs != null) {
      json[r'pairs'] = LocalApiClient.serialize(pairs);
    }
    return json;
  }

  static List<Strategy> listFromJson(List<dynamic> json) {
    return json == null
        ? <Strategy>[]
        : json.map((value) => Strategy.fromJson(value)).toList();
  }

  static Map<String, Strategy> mapFromJson(Map<String, dynamic> json) {
    final map = <String, Strategy>{};
    if (json != null && json.isNotEmpty) {
      json.forEach(
          (String key, dynamic value) => map[key] = Strategy.fromJson(value));
    }
    return map;
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }

    if (other is Strategy && runtimeType == other.runtimeType) {
      return name == other.name &&
          value == other.value &&
          const ListEquality().equals(pairs, other.pairs);
    }

    return false;
  }

  @override
  int get hashCode {
    var hashCode = runtimeType.hashCode;

    if (name != null) {
      hashCode = hashCode ^ name.hashCode;
    }

    if (value != null) {
      hashCode = hashCode ^ value.hashCode;
    }

    hashCode = hashCode ^ const ListEquality().hash(pairs);

    return hashCode;
  }

  Strategy copyWith({
    StrategyNameType name,
    dynamic value,
    List<StrategyPair> pairs,
  }) {
    Strategy copy = Strategy();
    copy.name = name ?? this.name;
    copy.value = value ?? this.value;
    {
      var newVal;
      final v = pairs ?? this.pairs;
      newVal = <StrategyPair>[]
        ..addAll((v ?? []).map((y) => y.copyWith()).toList());
      copy.pairs = newVal;
    }
    return copy;
  }
}
