// zxcvbn's dictionaries are about 0.8MB of the web build, and only the password screens need them, so the library is
// deferred: the JavaScript build downloads it the first time a password is scored.
import 'package:logging/logging.dart';
import 'package:zxcvbn/zxcvbn.dart' deferred as zxcvbn;

final _log = Logger('password-strength');

Future<void>? _loading;

/// Scores [password] from 0 (weak) to 4 (strong), loading zxcvbn first if need be. Null if it could not be scored, as
/// when zxcvbn could not be downloaded (the next call tries again).
Future<double?> passwordStrength(String password) async {
  try {
    await (_loading ??= zxcvbn.loadLibrary());
  } catch (e) {
    _log.warning('Unable to load the password strength checker', e);
    _loading = null;
    return null;
  }
  return zxcvbn.Zxcvbn().evaluate(password).score;
}
