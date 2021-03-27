import 'package:dio/dio.dart';
import 'package:featurehub_client_api/api.dart';
import 'package:featurehub_client_sdk/featurehub.dart';
import 'package:openapi_dart_common/openapi.dart';

class FeatureHubSimpleApi {
  final List<String> _environmentIds;
  final FeatureServiceApi _api;
  final ClientFeatureRepository _repository;
  String? xFeaturehubHeader;

  FeatureHubSimpleApi(String host, this._environmentIds, this._repository)
      : assert(host != null),
        assert(_environmentIds != null && _environmentIds.isNotEmpty),
        assert(_repository != null),
        _api = FeatureServiceApi(ApiClient(basePath: host)) {
    _repository.clientContext.registerChangeHandler((header) async {
      xFeaturehubHeader = header;
    });
  }

  Future<ClientFeatureRepository> request() async {
    return _api
        .getFeatureStates(_environmentIds,
            options: xFeaturehubHeader == null
                ? null
                : (Options()..headers!['x-featurehub'] = xFeaturehubHeader))
        .then((environments) {
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
