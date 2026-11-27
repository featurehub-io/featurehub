/// The few browser APIs the admin app uses, through package:web on the web (JavaScript and WebAssembly) and as
/// no-ops elsewhere (the tests run on the Dart VM).
library;

export 'browser_stub.dart' if (dart.library.js_interop) 'browser_web.dart';
