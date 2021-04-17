

import 'package:openapi_dart_common/openapi.dart';

class Host {
  String apiHost;
  ApiClient _apiClient;

  ApiClient get apiClient => _apiClient;

  Host({this.apiHost}) {
    _apiClient = ApiClient(basePath: this.apiHost);
  }


}
