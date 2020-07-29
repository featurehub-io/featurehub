import 'package:featurehub_client_api/api.dart';
import 'package:featurehub_client_sdk/featurehub.dart';
import 'package:openapi_dart_common/openapi.dart';

class FeatureHubSimpleApi {
  final List<String> _environmentIds;
  final FeatureServiceApi _api;
  final ClientFeatureRepository _repository;

  FeatureHubSimpleApi(String host, this._environmentIds, this._repository)
      : assert(host != null),
        assert(_environmentIds != null && _environmentIds.isNotEmpty),
        assert(_repository != null),
        _api = FeatureServiceApi(new ApiClient(basePath: host));

  Future<ClientFeatureRepository> request() async {
    return _api.getFeatureStates(_environmentIds).then((environments) {
      final states = <FeatureState>[];
      environments.forEach((e) {
        e.features.forEach((f) {
          f.environmentId = e.id;
        });
        states.addAll(e.features);
      });
      _repository.notify(SSEResultState.features, states);
      return _repository;
    }).catchError((e, s) {
      _repository.notify(SSEResultState.failure, null);
      return _repository;
    });
  }
}
