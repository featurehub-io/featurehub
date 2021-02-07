part of featurehub_client_api.api;

enum SSEResultState { ack, bye, failure, features, feature, deleteFeature }

extension SSEResultStateExtension on SSEResultState {
  String get name => toMap[this];

  // you have to call this extension class to use this as this is not yet supported
  static SSEResultState type(String name) => fromMap[name];

  static Map<String, SSEResultState> fromMap = {
    'ack': SSEResultState.ack,
    'bye': SSEResultState.bye,
    'failure': SSEResultState.failure,
    'features': SSEResultState.features,
    'feature': SSEResultState.feature,
    'delete_feature': SSEResultState.deleteFeature
  };
  static Map<SSEResultState, String> toMap = {
    SSEResultState.ack: 'ack',
    SSEResultState.bye: 'bye',
    SSEResultState.failure: 'failure',
    SSEResultState.features: 'features',
    SSEResultState.feature: 'feature',
    SSEResultState.deleteFeature: 'delete_feature'
  };

  static SSEResultState fromJson(dynamic data) =>
      data == null ? null : fromMap[data];

  dynamic toJson() => toMap[this];

  static List<SSEResultState> listFromJson(List<dynamic> json) => json == null
      ? <SSEResultState>[]
      : json.map((value) => fromJson(value)).toList();

  static SSEResultState copyWith(SSEResultState instance) => instance;

  static Map<String, SSEResultState> mapFromJson(Map<String, dynamic> json) {
    final map = <String, SSEResultState>{};
    if (json != null && json.isNotEmpty) {
      json.forEach((String key, dynamic value) => map[key] = fromJson(value));
    }
    return map;
  }
}
