import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

class AppsBloc implements Bloc {
  ManagementRepositoryClientBloc mrClient;

  var _applicationServiceApi;

  StreamSubscription<List<Application>> _currentApplicationsListListener;

  AppsBloc(this.mrClient) : assert(mrClient != null) {
    _applicationServiceApi = ApplicationServiceApi(mrClient.apiClient);
    _currentApplicationsListListener = mrClient
        .streamValley.currentPortfolioApplicationsStream
        .listen(_getCurrentPortfolioApplications);
  }

  final _currentApplicationsStream = BehaviorSubject<List<Application>>();

  BehaviorSubject<List<Application>> get currentApplicationsStream =>
      _currentApplicationsStream;

  Future<void> _getCurrentPortfolioApplications(
      List<Application> appList) async {
    _currentApplicationsStream.add(appList);
  }

  Future<void> createApplication(
      String applicationName, String appDescription) async {
    final application = Application();
    application.name = applicationName;
    application.description = appDescription;
    final newApp = await _applicationServiceApi.createApplication(
        mrClient.currentPid, application);
    await mrClient.streamValley.getCurrentPortfolioApplications();
    mrClient.setCurrentAid(newApp.id);
  }

  Future<void> updateApplication(Application application, String updatedAppName,
      String updateDescription) async {
    application.name = updatedAppName;
    application.description = updateDescription;
    return _applicationServiceApi
        .updateApplication(application.id, application)
        .then((onSuccess) async {
      await mrClient.streamValley.getCurrentPortfolioApplications();
    });
  }

  Future<bool> deleteApp(String appId) async {
    var success = false;
    try {
      success = await _applicationServiceApi.deleteApplication(appId);
      await mrClient.streamValley.getCurrentPortfolioApplications();
    } catch (e, s) {
      mrClient.dialogError(e, s);
    }
    ;
    return success;
  }

  @override
  void dispose() {
    _currentApplicationsStream.close();
    _currentApplicationsListListener.cancel();
  }
}
