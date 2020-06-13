import 'package:app_singleapp/util.dart';
import 'package:featurehub_client_sdk/featurehub.dart';

class EventsCommon {
  ClientFeatureRepository _repository;
  EventSourceRepositoryListener _eventSource;

  EventsCommon() {}

  void setAppSdkUrl(String url) {
    if (_eventSource != null) {
      _eventSource.close();
    }

    _repository = ClientFeatureRepository();

    String sdkUrl = baseUrl() + '/features/' + url;

    print("connecting to $sdkUrl");
    _eventSource = EventSourceRepositoryListener(sdkUrl, _repository);
  }

  ClientFeatureRepository get repository => _repository;

  void close() {
    if (_eventSource != null) {
      _eventSource.close();
      _eventSource = null;
    }
  }
}
