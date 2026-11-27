import 'package:web/web.dart' as web;

/// Sends the browser to [url], replacing the app.
void navigateTo(String url) {
  web.window.location.href = url;
}

/// Opens [url] in the browser window (tab) named [windowName].
void openWindow(String url, String windowName) {
  web.window.open(url, windowName);
}

/// The path of the page the browser is on, if there is a browser.
String? get locationPathname => web.window.location.pathname;

/// The origin of the page the browser is on, if there is a browser.
String? get locationOrigin => web.window.location.origin;

/// The page's base URL (from its `<base>` tag, else its own URL), if there is a browser.
String? get documentBaseUri => web.document.baseURI;
