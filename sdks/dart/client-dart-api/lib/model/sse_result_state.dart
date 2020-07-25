part of featurehub_client_api.api;

enum SSEResultState { ack, bye, failure, features, feature, delete_feature }

class SSEResultStateTypeTransformer {
  static Map<String, SSEResultState> fromJsonMap = {
    'ack': SSEResultState.ack,
    'bye': SSEResultState.bye,
    'failure': SSEResultState.failure,
    'features': SSEResultState.features,
    'feature': SSEResultState.feature,
    'delete_feature': SSEResultState.delete_feature
  };
  static Map<SSEResultState, String> toJsonMap = {
    SSEResultState.ack: 'ack',
    SSEResultState.bye: 'bye',
    SSEResultState.failure: 'failure',
    SSEResultState.features: 'features',
    SSEResultState.feature: 'feature',
    SSEResultState.delete_feature: 'delete_feature'
  };

  static SSEResultState fromJson(dynamic data) {
    var found = fromJsonMap[data];
    if (found == null) {
      throw ('Unknown enum value to decode: $data');
    }
    return found;
  }

  static dynamic toJson(SSEResultState data) {
    return toJsonMap[data];
  }

  static List<SSEResultState> listFromJson(List<dynamic> json) {
    return json == null
        ? <SSEResultState>[]
        : json.map((value) => fromJson(value)).toList();
  }

  static SSEResultState copyWith(SSEResultState instance) {
    return instance;
  }

  static Map<String, SSEResultState> mapFromJson(Map<String, dynamic> json) {
    final map = <String, SSEResultState>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) => map[key] = fromJson(value));
    }
    return map;
  }
}
