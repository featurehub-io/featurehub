import 'dart:async';

import 'package:dio/dio.dart';
import 'package:featurehub_client_api/api.dart';
import 'package:logging/logging.dart';

import 'repository.dart';

final _log = Logger('FeatureHub_GAListener');
final _GA_KEY = '_gaev_value';

void insertGoogleAnalyticsEvValue(Map<String, Object> other, String gaValue) {
  other[_GA_KEY] = gaValue;
}

class GoogleAnalyticsListener {
  final ClientFeatureRepository _repository;
  final String ua;
  String _cid;
  StreamSubscription<AnalyticsEvent> _analyticsListener;
  final GoogleAnalyticsApiClient _apiClient;

  GoogleAnalyticsListener(ClientFeatureRepository repository, this.ua,
      {String cid, GoogleAnalyticsApiClient apiClient})
      : _repository = repository,
        this._cid = cid,
        this._apiClient = apiClient ?? GoogleAnalyticsDioApiClient(),
        assert(ua != null),
        assert(repository != null) {
    _analyticsListener = _repository.analyticsEvent.listen(_analyticsPublisher);
  }

  String get cid => _cid;

  set cid(String value) {
    _cid = value;
  }

  void dispose() {
    _analyticsListener.cancel();
    _analyticsListener = null;
  }

  void _analyticsPublisher(AnalyticsEvent event) {
    final finalCid = event.other['cid']?.toString() ?? _cid;

    if (finalCid == null) {
      _log.severe('Unable to log GA event as no CID provided.');
    }

    final ev = event.other.containsKey(_GA_KEY)
        ? '&ev=' + Uri.encodeQueryComponent(event.other[_GA_KEY] ?? '')
        : '';

    String batchData = "";

    final baseForEachLine = 'v=1&tid=' +
        ua +
        "&cid=" +
        finalCid +
        "&t=event&ec=FeatureHub%20Event&ea=" +
        Uri.encodeQueryComponent(event.action) +
        ev +
        "&el=";

    event.features.forEach((f) {
      String line;
      switch (f.type) {
        case FeatureValueType.BOOLEAN:
          line = f.booleanValue ? "on" : "off";
          break;
        case FeatureValueType.STRING:
          line = f.stringValue;
          break;
        case FeatureValueType.NUMBER:
          line = f.numberValue.toString();
          break;
        case FeatureValueType.JSON:
          line = null;
          break;
      }

      if (line != null) {
        batchData +=
            baseForEachLine + Uri.encodeQueryComponent('${f.key} : $line\n');
      }
    });

    if (batchData.isNotEmpty) {
      _apiClient.postAnalyticBatch(batchData);
    }
  }
}

abstract class GoogleAnalyticsApiClient {
  void postAnalyticBatch(String data);
}

class GoogleAnalyticsDioApiClient implements GoogleAnalyticsApiClient {
  final Dio _dio;

  GoogleAnalyticsDioApiClient() : _dio = new Dio();

  @override
  void postAnalyticBatch(String data) {
    _dio
        .post('https://www.google-analytics.com/batch',
            data: data,
            options:
                new Options(contentType: 'application/x-www-form-urlencoded'))
        .catchError((e, s) => _log.severe('Failed to update GA', e, s));
  }
}
