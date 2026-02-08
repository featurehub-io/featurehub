// ignore: avoid_web_libraries_in_flutter
import 'package:universal_html/html.dart';

import 'package:open_admin_app/api/web_interface/url_handler.dart';

AbstractWebInterface getUrlAuthInstance() => WebInterface();

class WebInterface extends AbstractWebInterface {
  @override
  String homeUrl(bool overrideOrigin) {
    // try using the <base>  tag from the html doc first if there is one
    // as it will tell us the actual base url of the backend apis
    final origin = document.baseUri ?? window.location.origin;
    originUri = Uri.parse(window.location.origin);
    if (overrideOrigin) {
      final url = Uri.parse(origin);
      final apiUrl =
          '${originUri!.scheme}://${originUri!.host}:8903${url.path}';
      return (apiUrl.endsWith("/"))
          ? apiUrl.substring(0, apiUrl.length - 1)
          : apiUrl;
    } else if (overrideOrigin && origin.startsWith('http://[::1]')) {
      return 'http://[::1]:8903';
    } else {
      final url = Uri.parse(origin);
      if (url.path.endsWith("/")) {
        return url
            .replace(path: url.path.substring(0, url.path.length - 1))
            .toString();
      } else {
        return url.replace(path: url.path).toString();
      }
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
