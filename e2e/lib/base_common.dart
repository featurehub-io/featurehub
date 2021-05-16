import 'package:e2e_tests/util.dart';
import 'package:openapi_dart_common/openapi.dart';

class BaseApi {
  ApiClient _apiClient;

  BaseApi() : _apiClient = ApiClient(basePath: baseUrl()) {}

  ApiClient get apiClient => _apiClient;
}
