import 'dart:html' as html;

import 'package:bloc_provider/bloc_provider.dart';
import 'package:featurehub_client_api/api.dart';
import 'package:featurehub_client_sdk/featurehub.dart';
import 'package:featurehub_client_sdk/featurehub_get.dart';

class DisplayValue {
  final String value;
  final bool overridden;
  final FeatureValueType type;

  DisplayValue(this.value, this.overridden, this.type);
}

class RepositoryLoaderBloc extends Bloc {
  ClientFeatureRepository _repository = ClientFeatureRepository();
  String _host;
  String _environment;
  FeatureHubSimpleApi _simpleApi;

  RepositoryLoaderBloc() {
    html.window.addEventListener('storage', (event) {
      if (event is html.StorageEvent) {
        if (event != null && event.key == 'fh_url' && event.newValue != null) {
          final parts = event.newValue.split('features');
          // environment has changed
          if (parts != null && parts.length == 2) {
            if (_host != parts[0].substring(0, parts[0].length - 1) ||
                _environment != parts[1].substring(1)) {
              init();
            }
          }
        }
      }
    });
  }

  set sdkUrl(String sdkUrl) {
    html.window.localStorage['fh_url'] = sdkUrl;
    init();
  }

  bool hasStoredEnvironment() {
    return html.window.localStorage['fh_url'] != null;
  }

  String previousStoredEnvironmentHost() {
    final fhUrl = html.window.localStorage['fh_url'];

    if (fhUrl != null) {
      final parts = fhUrl.split('features');
      if (parts != null && parts.length == 2) {
        return parts[0] + "features/";
      }
    }

    return '';
  }

  void init() async {
    final fhUrl = html.window.localStorage['fh_url'];

    // swap back to notready
    _repository.notify(SSEResultState.bye, null);

    if (fhUrl != null) {
      final parts = fhUrl.split('features');
      if (parts != null && parts.length == 2) {
        _host = parts[0].substring(0, parts[0].length - 1);
        _environment = parts[1].substring(1);

        _repository.shutdownFeatures();

        // print("actual url ${_host}/features?sdkUrl=$_environment");

        _simpleApi = FeatureHubSimpleApi(_host, [_environment], _repository);
        _simpleApi.request();
      } else {
        _repository.notify(SSEResultState.failure, null);
      }
    } else {
      _repository.notify(SSEResultState.failure, null);
    }
  }

  Future<void> refresh() async {
    _simpleApi.request();
  }

  ClientFeatureRepository get repository => _repository;

  @override
  void dispose() {}

  DisplayValue getDisplayValue(String keyField, {bool nullReturn = false}) {
    final fs = _repository.feature(keyField);

    final nullVal = html.window.localStorage['fh_null_$keyField'];
    if (nullVal != null) {
      return DisplayValue(
          fs.type == FeatureValueType.BOOLEAN ? 'Off' : null, true, fs.type);
    }

    final value = html.window.localStorage['fh_value_$keyField'];
    if (value != null) {
      return DisplayValue(
          fs.type == FeatureValueType.BOOLEAN
              ? (value == 'true' ? 'On' : 'Off')
              : value,
          true,
          fs.type);
    }

    switch (fs.type) {
      case FeatureValueType.BOOLEAN:
        return DisplayValue(fs.value == true ? 'On' : 'Off', false, fs.type);
      case FeatureValueType.STRING:
      case FeatureValueType.NUMBER:
      case FeatureValueType.JSON:
        return DisplayValue(
            fs.value == null ? (nullReturn ? null : '') : fs.value.toString(),
            false,
            fs.type);
    }
  }

  void setValue(String keyField, dynamic replacement) {
    final fs = _repository.feature(keyField);
    if (fs.value == replacement) {
      html.window.localStorage.remove('fh_value_$keyField');
      html.window.localStorage.remove('fh_null_$keyField');
    } else if (replacement == null) {
      html.window.localStorage.remove('fh_value_$keyField');
      html.window.localStorage['fh_null_$keyField'] = 'null';
    } else {
      html.window.localStorage.remove('fh_null_$keyField');
      html.window.localStorage['fh_value_$keyField'] = replacement.toString();
    }
  }

  void reset() {
    _repository.availableFeatures.forEach((keyField) {
      html.window.localStorage.remove('fh_value_$keyField');
      html.window.localStorage.remove('fh_null_$keyField');
    });

    refresh();
  }
}
