import 'dart:io';

import 'package:app_singleapp/api/web_interface/url_handler.dart';

AbstractWebInterface getUrlAuthInstance() => IOWebInterface();

class IOWebInterface extends AbstractWebInterface {
  @override
  String homeUrl(bool overrideOrigin) {
    originUri = Uri.parse('http://localhost:8903');
    return Platform.isIOS ? 'http://localhost:8903' : 'http://10.0.2.2:8903';
  }

  @override
  void setOrigin() {}
  @override
  void authenticateViaProvider(String redirectUrl) {}
  @override
  String getStoredAuthToken() {
    return null;
  }

  @override
  void setStoredAuthToken(String token) {}
  @override
  void clearStoredAuthToken() {}
}
