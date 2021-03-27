import 'package:featurehub_client_api/api.dart';

typedef ClientContextChangedHandler = Future<void> Function(String header);

class ClientContext {
  final _attributes = <String, List<String>>{};
  final _handlers = <ClientContextChangedHandler>[];

  ClientContext userKey(String userkey) {
    _attributes['userkey'] = [userkey];
    return this;
  }

  ClientContext sessionKey(String sessionKey) {
    _attributes['session'] = [sessionKey];
    return this;
  }

  ClientContext country(StrategyAttributeCountryName countryName) {
    _attributes['country'] = [countryName.name];
    return this;
  }

  ClientContext device(StrategyAttributeDeviceName device) {
    _attributes['device'] = [device.name];
    return this;
  }

  ClientContext platform(StrategyAttributePlatformName platform) {
    _attributes['platform'] = [platform.name];
    return this;
  }

  ClientContext version(String version) {
    _attributes['version'] = [version];
    return this;
  }

  ClientContext attr(String key, String value) {
    _attributes[key] = [value];
    return this;
  }

  ClientContext attrs(key, List<String> values) {
    _attributes[key] = values;
    return this;
  }

  ClientContext clear() {
    _attributes.clear();
    return this;
  }

  void build() {
    final header = generateHeader();
    for (var handler in _handlers) {
      handler(header);
    }
  }

  String generateHeader() {
    if (_attributes.isEmpty) {
      return null;
    }

    var params = _attributes.entries.map((entry) {
      return entry.key + '=' + Uri.encodeQueryComponent(entry.value.join(','));
    }).toList();
    params.sort(); // i so hate the sort function
    return params.join(',');
  }

  Future<Function> registerChangeHandler(
      ClientContextChangedHandler handler) async {
    _handlers.add(
        handler); // have to do this first in case other code triggers before this callback

    try {
      await handler(generateHeader());
      return () => {_handlers.remove(handler)};
    } catch (e) {
      _handlers.remove(handler);
      return () => {};
    }
  }
}
