import 'dart:io';

import 'package:dio/dio.dart';
import 'package:e2e_tests/util.dart';
import 'package:featurehub_client_sdk/featurehub_get.dart';
import 'package:featurehub_client_sdk/featurehub_io.dart';

class EventsCommon {
  ClientFeatureRepository? _repository;
  EventSourceRepositoryListener? _eventSource;
  Dio dio;

  EventsCommon() : dio = Dio() {}

  String _baseUrl() => Platform.environment['FEATUREHUB_EDGE_URL'] ?? baseUrl();

  void setAppSdkUrl(String url) {
    if (_eventSource != null) {
      _eventSource!.close();
    }

    _repository = ClientFeatureRepository();

    String sdkUrl = _baseUrl() + '/features/' + url;

    print("connecting to $sdkUrl");
    _eventSource = EventSourceRepositoryListener(_baseUrl(), url, _repository!);
  }

  Future<Response<void>> optionsCheck(String apiKey) async {
    String url = "${_baseUrl()}/features?apiKey=${apiKey}";
    return await dio.request(url,
        options: Options(
            method: 'OPTIONS', headers: {'Origin': 'http://localhost:4200'}));
  }

  Future<ClientFeatureRepository> pollRepository(String apiKey) async {
    _repository ??= ClientFeatureRepository();

    final client = FeatureHubSimpleApi(_baseUrl(), [apiKey], _repository!);

    return client.request();
  }

  ClientFeatureRepository? get repository => _repository;

  void close() {
    if (_eventSource != null) {
      _repository = null;
      _eventSource!.close();
      _eventSource = null;
    }
  }
}
