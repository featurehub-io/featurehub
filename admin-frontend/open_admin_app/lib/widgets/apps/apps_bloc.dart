import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/widgets.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:rxdart/rxdart.dart';

class AppsBloc implements Bloc {
  final ManagementRepositoryClientBloc mrClient;

  late ApplicationServiceApi _applicationServiceApi;
  late StreamSubscription<List<Application>> _currentApplicationsListListener;

  AppsBloc(this.mrClient) {
    _applicationServiceApi = ApplicationServiceApi(mrClient.apiClient);
    _currentApplicationsListListener = mrClient
        .streamValley.currentPortfolioApplicationsStream
        .listen(_getCurrentPortfolioApplications);
    mrClient.streamValley.includeEnvironmentsInApplicationRequest = true;


    // this tells the mrClient to run any callback code after the page has finished loading
    WidgetsBinding.instance
        .addPostFrameCallback((_) => mrClient.processLandingActions());
  }

  // make sure we load apps with the features
  Future<void> init() async {
    await _refreshApplications();
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
    final application =
        CreateApplication(name: applicationName, description: appDescription);
    final newApp = await _applicationServiceApi.createApplication(
        mrClient.currentPid!, application);
    await mrClient.requestOwnDetails();
    await _refreshApplications();
    mrClient.setCurrentAid(newApp.id);
  }

  Future<void> _refreshApplications() async {
    await mrClient.streamValley.getCurrentPortfolioApplications();
  }

  Future<void> updateApplication(Application application, String updatedAppName,
      String updateDescription) async {
    application.name = updatedAppName;
    application.description = updateDescription;
    return _applicationServiceApi
        .updateApplicationOnPortfolio(application.portfolioId, application)
        .then((onSuccess) async {
      await _refreshApplications();
    });
  }

  Future<bool> deleteApp(String appId) async {
    var success = false;
    try {
      success = await _applicationServiceApi.deleteApplication(appId);
      await _refreshApplications();
    } catch (e, s) {
      await mrClient.dialogError(e, s);
    }

    return success;
  }

  @override
  void dispose() {
    mrClient.streamValley.includeEnvironmentsInApplicationRequest = false;
    _currentApplicationsListListener.cancel();
    _currentApplicationsStream.close();
  }

  void refreshPortfolioCache() {
    final id = mrClient.streamValley.currentPortfolio.portfolio.id;
    if (id != null) {
      CacheServiceApi(mrClient.apiClient).cacheRefresh(CacheRefreshRequest(portfolioId: [id]));
    }
  }

  void refreshApplicationCache(String appId) {
    CacheServiceApi(mrClient.apiClient).cacheRefresh(CacheRefreshRequest(applicationId: [appId]));
  }
}
