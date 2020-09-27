import 'dart:html' as html;

import 'package:bloc_provider/bloc_provider.dart';
import 'package:featurehub_client_api/api.dart';
import 'package:featurehub_client_sdk/featurehub.dart';
import 'package:featurehub_client_sdk/featurehub_get.dart';

class RepositoryLoaderBloc extends Bloc {
  ClientFeatureRepository _repository = ClientFeatureRepository();
  String _host;
  String _environment;
  FeatureHubSimpleApi _simpleApi;

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

  String getDisplayValue(String keyField, {bool nullReturn = false}) {
    final fs = _repository.getFeatureState(keyField);

    final nullVal = html.window.localStorage['fh_null_$keyField'];
    if (nullVal != null) {
      return fs.type == FeatureValueType.BOOLEAN ? 'Off' : null;
    }

    final value = html.window.localStorage['fh_value_$keyField'];
    if (value != null) {
      return fs.type == FeatureValueType.BOOLEAN
          ? (value == 'true' ? 'On' : 'Off')
          : value;
    }

    switch (fs.type) {
      case FeatureValueType.BOOLEAN:
        return fs.value == true ? 'On' : 'Off';
      case FeatureValueType.STRING:
      case FeatureValueType.NUMBER:
      case FeatureValueType.JSON:
        return fs.value == null
            ? (nullReturn ? null : '')
            : fs.value.toString();
    }
  }

  void setValue(String keyField, dynamic replacement) {
    final fs = _repository.getFeatureState(keyField);
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
