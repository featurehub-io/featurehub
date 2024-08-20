import 'dart:convert';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:dio/dio.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:rxdart/rxdart.dart';

class SystemConfigBloc implements Bloc {
  final ManagementRepositoryClientBloc mrClient;
  final SystemConfigServiceApi systemConfigServiceApi;
  final knownConfigs = BehaviorSubject<List<SystemConfig>?>.seeded(null);

  Stream<List<SystemConfig>?> get knownConfigStream => knownConfigs.stream;

  SystemConfigBloc(this.mrClient)
      : systemConfigServiceApi = SystemConfigServiceApi(mrClient.apiClient) {
    _loadConfigs();
  }

  /**
   * this list does not change between restarts of the app due to system config changes
   */
  Future _loadConfigs() async {
    final configs = await systemConfigServiceApi.getSystemConfig();

    knownConfigs.value = configs.configs;
  }

  @override
  void dispose() {
    knownConfigs.close();
  }

  Future<String?> knownSiteRedirectUrl(String externalOrganisationUrl) async {
    final data = await mrClient.apiClient.invokeAPI(
        externalOrganisationUrl, [], null, ['bearerAuth'], Options());
    if (data.statusCode == 200 && data.body != null) {
      return await utf8.decodeStream(data.body!);
    }
    return null;
  }
}
