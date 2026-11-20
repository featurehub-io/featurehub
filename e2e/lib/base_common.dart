import 'dart:io';

import 'package:e2e_tests/util.dart';
import 'package:openapi_dart_common/openapi.dart';

class BaseApi {
  ApiClient _apiClient;

  BaseApi() : _apiClient = ApiClient(basePath: baseUrl()) {
    checkLoggingForApiClient(_apiClient);
  }

  ApiClient get apiClient => _apiClient;
}
