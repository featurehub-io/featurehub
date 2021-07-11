import 'package:openapi_dart_common/openapi.dart';

class Host {
  String apiHost;
  ApiClient _apiClient;

  ApiClient get apiClient => _apiClient;

  Host({required this.apiHost}) : _apiClient = ApiClient(basePath: apiHost);
}
