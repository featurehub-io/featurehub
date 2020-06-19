import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:pedantic/pedantic.dart';
import 'package:rxdart/rxdart.dart' hide Notification;

class FeatureStatusFeatures {
  final ApplicationFeatureValues applicationFeatureValues;
  List<String> sortedByNameEnvironmentIds;
  // envId, EnvironmentFeatureValues mapping - so it is done only once not once per line in table
  Map<String, EnvironmentFeatureValues> applicationEnvironments = {};
  List<String> visibleEnvironments = [];

  FeatureStatusFeatures(this.applicationFeatureValues) {
    sortedByNameEnvironmentIds = applicationFeatureValues.environments
        .map((e) => e.environmentId)
        .toList();

    applicationFeatureValues.environments.forEach((e) {
      applicationEnvironments[e.environmentId] = e;
    });
  }
}

class LineStatusFeature {
  Feature feature;
  List<EnvironmentFeatureValues> environmentFeatureValues; // shrunk to
  List<String> sortedByNameEnvironmentIds;
  Map<String, EnvironmentFeatureValues> applicationEnvironments = {};

  LineStatusFeature(ApplicationFeatureValues afv, String featureId) {
    sortedByNameEnvironmentIds =
        afv.environments.map((e) => e.environmentId).toList();
    feature = afv.features.firstWhere((element) => element.id == featureId);
    environmentFeatureValues = afv.environments;
    environmentFeatureValues.forEach((e) {
      applicationEnvironments[e.environmentId] = e;
    });
  }
}

class RefreshFeatureNotification extends Notification {
  final String featureId;

  const RefreshFeatureNotification({this.featureId});
}

class FeatureStatusBloc implements Bloc {
  String portfolioId;
  String applicationId;
  final ManagementRepositoryClientBloc mrClient;
  ApplicationServiceApi _appServiceApi;
  EnvironmentServiceApi _environmentServiceApi;

  FeatureServiceApi _featureServiceApi;

  final _appSearchResultSource = BehaviorSubject<List<Application>>();
  Stream<List<Application>> get applications => _appSearchResultSource.stream;

  final _appFeatureValuesBS = BehaviorSubject<FeatureStatusFeatures>();
  Stream<FeatureStatusFeatures> get appFeatureValues =>
      _appFeatureValuesBS.stream;
  // feature-id, environments for feature
  final _lines = <String, BehaviorSubject<LineStatusFeature>>{};

  StreamSubscription<String> _currentPid;
  StreamSubscription<String> _currentAppId;

  Stream<LineStatusFeature> getLineStatus(String featureId) => _seedLineStatus(
          featureId, _appFeatureValuesBS.value.applicationFeatureValues)
      .stream;

  BehaviorSubject<LineStatusFeature> _seedLineStatus(
          String featureId, ApplicationFeatureValues afv) =>
      _lines.putIfAbsent(
          featureId,
          () => BehaviorSubject<LineStatusFeature>.seeded(
              LineStatusFeature(afv, featureId)));

  final _getAllAppValuesDebounceStream = BehaviorSubject<bool>();

  FeatureStatusBloc(this.mrClient) : assert(mrClient != null) {
    _appServiceApi = ApplicationServiceApi(mrClient.apiClient);
    _featureServiceApi = FeatureServiceApi(mrClient.apiClient);
    _environmentServiceApi = EnvironmentServiceApi(mrClient.apiClient);
    _currentPid = mrClient.streamValley.currentPortfolioIdStream
        .listen(addApplicationsToStream);
    _currentAppId = mrClient.streamValley.currentAppIdStream.listen(setAppId);
    _getAllAppValuesDebounceStream
        .debounceTime(Duration(milliseconds: 300))
        .listen((event) {
      _actuallyCallAddAppFeatureValuesToStream();
    });
  }

  void setAppId(String appId) {
    applicationId = appId;
    if (applicationId != null) {
      addAppFeatureValuesToStream();
    }
  }

  Future<Environment> getEnvironment(String envId) async {
    return _environmentServiceApi
        .getEnvironment(envId,
            includeServiceAccounts: true, includeSdkUrl: true)
        .catchError(mrClient.dialogError);
  }

  void _sortApplicationFeatureValues(
      ApplicationFeatureValues appFeatureValues) {
//    appFeatureValues.environments.sort((a, b) => a.environmentName.compareTo(b.environmentName));
    appFeatureValues.features.sort((a, b) => a.name.compareTo(b.name));
  }

  void _actuallyCallAddAppFeatureValuesToStream() async {
    try {
      final appFeatureValues = await _featureServiceApi
          .findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
              applicationId);
      if (!_appFeatureValuesBS.isClosed) {
        _sortApplicationFeatureValues(appFeatureValues);

        // make sure the line streams are updated and there before the whole list of lines gets published
        appFeatureValues.features.forEach((f) {
          _seedLineStatus(f.id, appFeatureValues);
        });

        _appFeatureValuesBS.add(FeatureStatusFeatures(appFeatureValues));
      }
    } catch (e, s) {
      mrClient.dialogError(e, s);
    }
  }

  void addAppFeatureValuesToStream() async {
    _getAllAppValuesDebounceStream.add(
        true); // tell it to ask for the data, but debounce it through a 300ms stream
  }

  void clearAppFeatureValuesStream() {
    if (!_appFeatureValuesBS.isClosed) {
      _appFeatureValuesBS.add(null);
    }
  }

  void checkPortfolioIdIsLegit(List<Portfolio> portfolios) {
    if (!portfolios.any((p) => p.id == portfolioId)) {
      //if portfolioId not found in the system - set it to null
      portfolioId = null;
      mrClient.customError(
          messageTitle: 'Provided portfolio ID is not found in the system');
    }
  }

  Future<void> addApplicationsToStream(String pid) async {
    portfolioId = pid;
    List<Application> appList;
    clearAppFeatureValuesStream();
    try {
      appList = await _appServiceApi.findApplications(portfolioId,
          order: SortOrder.ASC);
      if (!_appSearchResultSource.isClosed) {
        _appSearchResultSource.add(appList);
      }
    } catch (e, s) {
      mrClient.dialogError(e, s);
    }
    if (appList != null && applicationId != null) {
      checkApplicationIdIsLegit(appList);
    }
  }

  void checkApplicationIdIsLegit(List<Application> appList) {
    if (!appList.any((app) => app.id == applicationId)) {
      //if applicationId not found in the system - set it to null
      applicationId = null;
      mrClient.customError(
          messageTitle: 'Provided application ID is not found in the system');
    }
  }

  Future<void> createFeature(
      String name,
      String key,
      FeatureValueType featureValueType,
      String featureAlias,
      String featureLink) async {
    final feature = Feature()
      ..name = name
      ..valueType = featureValueType
      ..key = key
      ..alias = featureAlias
      ..link = featureLink;
    await _featureServiceApi.createFeaturesForApplication(
        applicationId, feature);
    unawaited(mrClient.streamValley.getCurrentApplicationFeatures());
    addAppFeatureValuesToStream();
  }

  Future<void> updateFeature(Feature feature, String newName, String newKey,
      String newFeatureAlias, String newFeatureLink) async {
    final currentFeature =
        await _featureServiceApi.getFeatureByKey(applicationId, feature.key);
    final newFeature = currentFeature
      ..name = newName
      ..alias = newFeatureAlias
      ..link = newFeatureLink
      ..key = newKey;
    await _featureServiceApi.updateFeatureForApplication(
        applicationId, feature.key, newFeature);
    addAppFeatureValuesToStream();
  }

  Future<void> deleteFeature(String key) async {
    await _featureServiceApi.deleteFeatureForApplication(applicationId, key);
    addAppFeatureValuesToStream();
    unawaited(mrClient.streamValley.getCurrentApplicationFeatures());
  }

  @override
  void dispose() {
    _appSearchResultSource.close();
    _appFeatureValuesBS.close();
    _currentPid.cancel();
    _currentAppId.cancel();
  }

  // in this case it will need to go and get everything again and just filter
  // down for what we actually have
  void refreshFeature(String featureId) async {
    try {
      final appFeatureValues = await _featureServiceApi
          .findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
              applicationId);
      getLineStatus(featureId);
      _sortApplicationFeatureValues(
          appFeatureValues); // make sure it is consistently sorted
      _lines[featureId].add(LineStatusFeature(appFeatureValues, featureId));
    } catch (e, s) {
      mrClient.dialogError(e, s);
    }
  }
}
