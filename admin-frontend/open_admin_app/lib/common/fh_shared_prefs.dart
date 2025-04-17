import 'package:mrapi/api.dart';
import 'package:shared_preferences/shared_preferences.dart';

abstract class FHSharedPrefsContract {
  Future<void> saveInt(String key, int value);
  Future<int?> getInt(String key);
  Future<bool?> getBool(String key);
  Future<void> saveBool(String key, bool value);
  Future<String?> getString(String key);
  Future<void> saveString(String key, String? value);

  Future<String?> getEmail();
  Future<void> setEmail(String value);
  Future<String?> currentPortfolioId();
  Future<void> setCurrentPortfolioId(String? id);
  Future<String?> currentApplicationId();
  Future<String?> currentEnvId();
  Future<void> setCurrentApplicationId(String? id);
  Future<void> setCurrentEnvId(String? id);

  Future<void> setPortfolioAndApplicationId(
      String? portfolioId, String? applicationId);
  // store the portfolio and return the application id
  Future<Application?> setPortfolio(Portfolio portfolio);
}

FHSharedPrefs prefs = FHSharedPrefs();

_FHSharedPrefs? _prefs;

class FHSharedPrefs extends FHSharedPrefsContract {
  FHSharedPrefs() {
    if (_prefs == null) {
      _FHSharedPrefs.getSharedInstance().then((p) => _prefs = p);
    }
  }

  @override
  Future<String?> currentApplicationId() async =>
      await _prefs?.currentApplicationId();

  @override
  Future<String?> currentEnvId() async => await _prefs?.currentEnvId();

  @override
  Future<String?> currentPortfolioId() async =>
      await _prefs?.currentPortfolioId();

  @override
  Future<bool?> getBool(String key) async => await _prefs?.getBool(key);

  @override
  Future<String?> getEmail() async => await _prefs?.getEmail();

  @override
  Future<int?> getInt(String key) async => await _prefs?.getInt(key);

  @override
  Future<void> saveBool(String key, bool value) async =>
      await _prefs?.saveBool(key, value);

  @override
  Future<void> saveInt(String key, int value) async =>
      await _prefs?.saveInt(key, value);

  @override
  Future<void> setCurrentApplicationId(String? id) async =>
      await _prefs?.setCurrentApplicationId(id);

  @override
  Future<void> setCurrentPortfolioId(String? id) async =>
      await _prefs?.setCurrentPortfolioId(id);

  @override
  Future<void> setEmail(String value) async => await _prefs?.setEmail(value);

  @override
  Future<void> setPortfolioAndApplicationId(
          String? portfolioId, String? applicationId) async =>
      await _prefs?.setPortfolioAndApplicationId(portfolioId, applicationId);

  @override
  Future<Application?> setPortfolio(Portfolio portfolio) async =>
      await _prefs?.setPortfolio(portfolio);

  void saveCurrentRoute(String json) => _prefs?.saveCurrentRoute(json);

  @override
  Future<String?> getString(String key) async => await _prefs?.getString(key);

  @override
  Future<void> saveString(String key, String? value) async =>
      _prefs?.saveString(key, value);

  @override
  Future<void> setCurrentEnvId(String? id) async =>
      await _prefs?.setCurrentEnvId(id);
}

const _keyEmail = 'lastUsername';
const _keyApplicationId = 'currentAid';
const _keyEnvId = 'currentEid';
const _keyPortfolioId = 'currentPid';
const _keyCurrentRoute = 'current-route';

class _FHSharedPrefs extends FHSharedPrefsContract {
  final SharedPreferences _prefs;

  static Future<_FHSharedPrefs> getSharedInstance(
      {SharedPreferences? prefs}) async {
    final sharedPrefs = prefs ?? await SharedPreferences.getInstance();
    return Future.value(_FHSharedPrefs._internal(sharedPrefs));
  }

  _FHSharedPrefs._internal(this._prefs);

  Future<void> clear() async {
    await _prefs.clear();
  }

  @override
  Future<String?> getEmail() async {
    return _prefs.getString(_keyEmail);
  }

  @override
  Future<void> setEmail(String value) async {
    final previousPerson = await getEmail();

    if (value != previousPerson) {
      await clear();
    }

    await _prefs.setString(_keyEmail, value);
  }

  @override
  Future<int?> getInt(String key) async {
    return _prefs.getInt(key);
  }

  @override
  Future<void> saveInt(String key, int value) async {
    await _prefs.setInt(key, value);
  }

  @override
  Future<bool?> getBool(String key) async {
    return _prefs.getBool(key);
  }

  @override
  Future<void> saveBool(String key, bool value) async {
    await _prefs.setBool(key, value);
  }

  @override
  Future<String?> currentEnvId() async => _prefs.getString(_keyEnvId);

  @override
  Future<String?> currentApplicationId() async =>
      _prefs.getString(_keyApplicationId);

  @override
  Future<String?> currentPortfolioId() async =>
      _prefs.getString(_keyPortfolioId);

  @override
  Future<void> setCurrentEnvId(String? id) async {
    if (id == null) {
      await _prefs.remove(_keyEnvId);
    } else {
      await _prefs.setString(_keyEnvId, id);
    }
  }

  @override
  Future<void> setCurrentApplicationId(String? id) async {
    if (id == null) {
      await _prefs.remove(_keyApplicationId);
    } else {
      await _prefs.setString(_keyApplicationId, id);
    }
  }

  @override
  Future<void> setCurrentPortfolioId(String? id) async {
    if (id == null) {
      await _prefs.remove(_keyPortfolioId);
    } else {
      await _prefs.setString(_keyPortfolioId, id);
    }
  }

  @override
  Future<void> setPortfolioAndApplicationId(
      String? portfolioId, String? applicationId) async {
    await setCurrentPortfolioId(portfolioId);
    await setCurrentApplicationId(applicationId);
  }

  @override
  Future<Application?> setPortfolio(Portfolio portfolio) async {
    await setCurrentPortfolioId(portfolio.id);

    if (portfolio.applications.isEmpty) {
      await setCurrentApplicationId(null);
      return null;
    }

    final appId = await currentApplicationId();
    final app = appId == null
        ? portfolio.applications[0]
        : portfolio.applications.firstWhere((a) => a.id == appId,
            orElse: () => portfolio.applications[0]);

    if (app.id != appId) {
      await setCurrentApplicationId(app.id);
    }

    return app;
  }

  saveCurrentRoute(String json) {
    _prefs.setString(_keyCurrentRoute, json);
  }

  @override
  Future<String?> getString(String key) async => _prefs.getString(key);

  @override
  Future<void> saveString(String key, String? value) async {
    if (value == null) {
      _prefs.remove(key);
    } else {
      _prefs.setString(key, value);
    }
  }
}
