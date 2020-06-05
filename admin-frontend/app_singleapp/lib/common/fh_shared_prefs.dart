import 'package:shared_preferences/shared_preferences.dart';

abstract class FHSharedPrefsContract {
  Future<void> saveString(String key, String value);
  Future<void> saveInt(String key, int value);
  Future<String> getString(String key);
  Future<int> getInt(String key);
  Future<bool> getBool(String key);
  Future<void> saveBool(String key, bool value);
  Future<void> delete(String key);
  Future<void> deleteAll();

  static Future<FHSharedPrefsContract> getSharedInstance() async {
    return await FHSharedPrefs.getSharedInstance();
  }
}

class FHSharedPrefs extends FHSharedPrefsContract {
  SharedPreferences _prefs;

  static Future<FHSharedPrefs> getSharedInstance(
      { SharedPreferences prefs }) async {
    final sharedPrefs = prefs ?? await SharedPreferences.getInstance();
    return Future.value(FHSharedPrefs._internal(sharedPrefs));
  }

  FHSharedPrefs._internal(this._prefs);

  Future<bool> saveString(String key, String value) async {
    return await _prefs.setString(key, value);
  }

  Future<String> getString(String key) async {
    return _prefs.getString(key);
  }

  Future<void> delete(String key) async {
    await _prefs.remove(key);
  }

  Future<void> deleteAll() async {
    await _prefs.clear();
  }

  @override
  Future<int> getInt(String key) async {
    return _prefs.getInt(key);
  }

  @override
  Future<void> saveInt(String key, int value) async {
    await _prefs.setInt(key, value);
  }

  @override
  Future<bool> getBool(String key) async {
    return _prefs.getBool(key);
  }

  @override
  Future<void> saveBool(String key, bool value) async {
    await _prefs.setBool(key, value);
  }
}
