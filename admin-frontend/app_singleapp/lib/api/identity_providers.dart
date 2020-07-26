import 'dart:html';

import 'package:app_singleapp/api/client_api.dart';
import 'package:mrapi/api.dart';

class IdentityProviders {
  List<String> _identityProviders = <String>['local'];

  set identityProviders(List<String> val) => _identityProviders = val;

  bool get hasLocal => _identityProviders.contains('local');
  bool get has3rdParty =>
      _identityProviders.where((p) => p != 'local').isNotEmpty;
  List<String> get externalProviders =>
      _identityProviders.where((p) => p != 'local').toList(growable: false);

  bool get hasMultiple3rdPartyProviders => externalProviders.length > 1;

  Map<String, String> externalProviderAssets = <String, String>{
    'oauth2-google':
        'assets/signup_3rdparty/btn_google_signin_dark_normal_web.png'
  };

  ManagementRepositoryClientBloc _bloc;

  set bloc(ManagementRepositoryClientBloc bloc) => _bloc = bloc;

  AuthServiceApi _authServiceApi;

  set authServiceApi(AuthServiceApi val) => _authServiceApi = val;

  void authenticateViaProvider(String provider) {
    _authServiceApi
        .getLoginUrlForProvider(provider)
        .then((value) => window.location.href = value.redirectUrl)
        .catchError(_bloc.dialogError);
  }
}
