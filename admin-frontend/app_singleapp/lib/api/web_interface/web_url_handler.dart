import 'dart:html';

import 'package:app_singleapp/api/web_interface/url_handler.dart';

AbstractWebInterface getUrlAuthInstance() => WebInterface();

class WebInterface extends AbstractWebInterface {
  @override
  String homeUrl(bool overrideOrigin) {
    final origin = window.location.origin;
    originUri = Uri.parse(window.location.origin);
    if (overrideOrigin) {
      return '${originUri!.scheme}://${originUri!.host}:8903';
    } else if (overrideOrigin && origin.startsWith('http://[::1]')) {
      return 'http://[::1]:8903';
    } else {
      final url = Uri.parse(origin);
      return url.replace(path: url.path).toString();
    }
  }

  @override
  void setOrigin() {
    originUri = Uri.parse(window.location.origin);
  }

  @override
  void authenticateViaProvider(String redirectUrl) {
    window.location.href = redirectUrl;
  }

  @override
  String? getStoredAuthToken() {
    final cookies = document.cookie!.split(';')
      ..retainWhere(
          (s) => s.trim().startsWith('${AbstractWebInterface.bearerToken}='));

    if (cookies.isNotEmpty) {
      return cookies.first
          .trim()
          .substring('${AbstractWebInterface.bearerToken}='.length);
    }

    return null;
  }

  @override
  void setStoredAuthToken(String token) {
    document.cookie = '${AbstractWebInterface.bearerToken}=$token; path=/';
  }

  @override
  void clearStoredAuthToken() {
    // expires back in 1970
    document.cookie =
        '${AbstractWebInterface.bearerToken}=Da; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/';
  }
}
