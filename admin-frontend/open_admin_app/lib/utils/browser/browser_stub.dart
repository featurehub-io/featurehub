/// Sends the browser to [url], replacing the app.
void navigateTo(String url) {}

/// Opens [url] in the browser window (tab) named [windowName].
void openWindow(String url, String windowName) {}

/// The path of the page the browser is on, if there is a browser.
String? get locationPathname => null;

/// The origin of the page the browser is on, if there is a browser.
String? get locationOrigin => null;

/// The page's base URL (from its `<base>` tag, else its own URL), if there is a browser.
String? get documentBaseUri => null;
