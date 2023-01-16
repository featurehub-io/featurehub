// ignore: avoid_web_libraries_in_flutter
import 'dart:html';

import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:openapi_dart_common/openapi.dart';

class ServerCapabilities {
  List<String> _identityProviders = <String>['local'];
  Map<String, IdentityProviderInfo> identityInfo = {};
  Map<String, String> _capabilities = {};

  set identityProviders(List<String> val) => _identityProviders = val;
  set capabilities(Map<String, String> val)  => _capabilities = val;

  bool get hasLocal => _identityProviders.contains('local');
  bool get has3rdParty =>
      _identityProviders.where((p) => p != 'local').isNotEmpty;

  // external providers are defined in the FH server properties file, e.g. oauth2.providers=oauth2-google,oauth2-azure,oauth2-github

  List<String> get externalProviders =>
      _identityProviders.where((p) => p != 'local').toList(growable: false);

  bool get hasMultiple3rdPartyProviders => externalProviders.length > 1;

  final ManagementRepositoryClientBloc bloc;
  AuthServiceApi? _authServiceApi;
  final ApiClient apiClient;

  ServerCapabilities(this.bloc, this.apiClient);

  void authenticateViaProvider(String provider) {
    _authServiceApi ??= AuthServiceApi(apiClient);

    _authServiceApi!.getLoginUrlForProvider(provider).then((value) {
      return window.location.href = value.redirectUrl!;
    }).catchError((e, s) {
      bloc.dialogError(e, s);
    });
  }

  /**
   * the server supports webhooks if it returns this
   */
  bool get capabilityWebhooks => _capabilities['webhook.features'] == 'true';
}
