import 'dart:html';

import 'package:app_singleapp/api/client_api.dart';
import 'package:mrapi/api.dart';

class IdentityProviders {
  List<String> _identityProviders = <String>['local'];

  set identityProviders(List<String> val) => _identityProviders = val;

  bool get hasLocal => _identityProviders.contains('local');
  bool get has3rdParty =>
      _identityProviders.where((p) => p != 'local').isNotEmpty;

  // external providers are defined in the FH server properties file, e.g. oauth2.providers=oauth2-google,oauth2-azure,oauth2-github

  List<String> get externalProviders =>
      _identityProviders.where((p) => p != 'local').toList(growable: false);

  bool get hasMultiple3rdPartyProviders => externalProviders.length > 1;

  final ManagementRepositoryClientBloc _bloc;
  final AuthServiceApi _authServiceApi;

  IdentityProviders(this._bloc, this._authServiceApi);

  void authenticateViaProvider(String provider) {
    _authServiceApi.getLoginUrlForProvider(provider).then((value) {
      return window.location.href = value.redirectUrl!;
    }).catchError((e, s) {
      _bloc.dialogError(e, s);
    });
  }
}
