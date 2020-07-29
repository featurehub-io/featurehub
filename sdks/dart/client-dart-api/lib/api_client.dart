part of featurehub_client_api.api;

class LocalApiClient {
  static final _regList = RegExp(r'^List<(.*)>$');
  static final _regMap = RegExp(r'^Map<String,(.*)>$');

  static dynamic serialize(Object value) {
    try {
      if (value == null) {
        return null;
      } else if (value is List) {
        return value.map((v) => serialize(v)).toList();
      } else if (value is Map) {
        return Map.fromIterables(
            value.keys, value.values.map((v) => serialize(v)));
      } else if (value is String) {
        return value;
      } else if (value is bool) {
        return value;
      } else if (value is num) {
        return value;
      } else if (value is DateTime) {
        return value.toUtc().toIso8601String();
      }
      if (value is Environment) {
        return value.toJson();
      }
      if (value is FeatureState) {
        return value.toJson();
      }
      if (value is FeatureStateUpdate) {
        return value.toJson();
      }
      if (value is FeatureValueType) {
        return FeatureValueTypeTypeTransformer.toJson(value);
      }
      if (value is RoleType) {
        return RoleTypeTypeTransformer.toJson(value);
      }
      if (value is SSEResultState) {
        return SSEResultStateTypeTransformer.toJson(value);
      }
      if (value is Strategy) {
        return value.toJson();
      }
      if (value is StrategyNameType) {
        return StrategyNameTypeTypeTransformer.toJson(value);
      }
      if (value is StrategyPair) {
        return value.toJson();
      }
      return value.toString();
    } on Exception catch (e, stack) {
      throw ApiException.withInner(
          500, 'Exception during deserialization.', e, stack);
    }
  }

  static dynamic deserializeFromString(String json, String targetType) {
    if (json == null) {
      // HTTP Code 204
      return null;
    }

    // Remove all spaces.  Necessary for reg expressions as well.
    targetType = targetType.replaceAll(' ', '');

    if (targetType == 'String') return json;

    var decodedJson = jsonDecode(json);
    return deserialize(decodedJson, targetType);
  }

  static dynamic deserialize(dynamic value, String targetType) {
    if (value == null) return null; // 204
    try {
      switch (targetType) {
        case 'String':
          return '$value';
        case 'int':
          return value is int ? value : int.parse('$value');
        case 'bool':
          return value is bool ? value : '$value'.toLowerCase() == 'true';
        case 'double':
          return value is double ? value : double.parse('$value');
        case 'Environment':
          return Environment.fromJson(value);
        case 'FeatureState':
          return FeatureState.fromJson(value);
        case 'FeatureStateUpdate':
          return FeatureStateUpdate.fromJson(value);
        case 'FeatureValueType':
          return FeatureValueTypeTypeTransformer.fromJson(value);
        case 'RoleType':
          return RoleTypeTypeTransformer.fromJson(value);
        case 'SSEResultState':
          return SSEResultStateTypeTransformer.fromJson(value);
        case 'Strategy':
          return Strategy.fromJson(value);
        case 'StrategyNameType':
          return StrategyNameTypeTypeTransformer.fromJson(value);
        case 'StrategyPair':
          return StrategyPair.fromJson(value);
        default:
          {
            Match match;
            if (value is List &&
                (match = _regList.firstMatch(targetType)) != null) {
              var newTargetType = match[1];
              return value.map((v) => deserialize(v, newTargetType)).toList();
            } else if (value is Map &&
                (match = _regMap.firstMatch(targetType)) != null) {
              var newTargetType = match[1];
              return Map.fromIterables(value.keys,
                  value.values.map((v) => deserialize(v, newTargetType)));
            }
          }
      }
    } on Exception catch (e, stack) {
      throw ApiException.withInner(
          500, 'Exception during deserialization.', e, stack);
    }
    throw ApiException(
        500, 'Could not find a suitable class for deserialization');
  }

  /// Format the given parameter object into string.
  static String parameterToString(dynamic value) {
    if (value == null) {
      return '';
    } else if (value is DateTime) {
      return value.toUtc().toIso8601String();
    } else if (value is String) {
      return value.toString();
    }

    if (value is FeatureValueType) {
      return FeatureValueTypeTypeTransformer.toJson(value).toString();
    }
    if (value is RoleType) {
      return RoleTypeTypeTransformer.toJson(value).toString();
    }
    if (value is SSEResultState) {
      return SSEResultStateTypeTransformer.toJson(value).toString();
    }
    if (value is StrategyNameType) {
      return StrategyNameTypeTypeTransformer.toJson(value).toString();
    }

    return jsonEncode(value);
  }
}
