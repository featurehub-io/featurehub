abstract class AbstractWebInterface {
  Uri? originUri;

  String homeUrl(bool overrideOrigin);

  void setOrigin() {}

  void authenticateViaProvider(String redirectUrl);

  static var bearerToken = 'bearer-token';

  String? getStoredAuthToken();

  void setStoredAuthToken(String token);

  void clearStoredAuthToken();
}
