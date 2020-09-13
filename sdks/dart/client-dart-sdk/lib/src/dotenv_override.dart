import 'repository.dart';

/// register environment overrides using a dotenv file
/// by default this will always allow lock overrides to force the
/// client to honour the value stored in the environment regardless
/// of what the FeatureHub server is sending.
class DotEnvOverride {
  final Map<String, String> env;

  DotEnvOverride(this.env, ClientFeatureRepository repository,
      {bool allowLockOverride = true}) {
    repository.registerFeatureValueInterceptor(
        allowLockOverride, (key) => _match(key));
  }

  ValueMatch _match(String key) {
    if (env.containsKey(key)) {
      return ValueMatch(true, env[key]);
    }

    return ValueMatch(false, null);
  }
}
