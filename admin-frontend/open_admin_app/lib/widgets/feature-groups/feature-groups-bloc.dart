import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/widgets.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/mr_client_aware.dart';
import 'package:rxdart/rxdart.dart';

class FeatureGroupsBloc implements Bloc, ManagementRepositoryAwareBloc {
  late FeatureGroupServiceApi featureGroupServiceApi;
  final ManagementRepositoryClientBloc _mrClient;

  late StreamSubscription<List<Application>> _currentApplicationsListListener;
  late StreamSubscription<String?> _currentApplicationListener;

  String? currentEnvId;

  FeatureGroupsBloc(this._mrClient) {
    _currentApplicationsListListener = mrClient
        .streamValley.currentPortfolioApplicationsStream
        .listen(_getCurrentPortfolioApplications);
    _currentApplicationListener = mrClient.streamValley.currentAppIdStream
        .listen(resetDataStreamsOnAppIdChange);
    mrClient.streamValley.includeEnvironmentsInApplicationRequest = true;
    featureGroupServiceApi = FeatureGroupServiceApi(_mrClient.apiClient);
    mrClient.streamValley.includeEnvironmentsInApplicationRequest = true;

    // this tells the mrClient to run any callback code after the page has finished loading
    WidgetsBinding.instance
        .addPostFrameCallback((_) => mrClient.processLandingActions());
  }

  final _featureGroupsStream =
      BehaviorSubject<List<FeatureGroupListGroup>>.seeded([]);
  BehaviorSubject<List<FeatureGroupListGroup>> get featureGroupsStream =>
      _featureGroupsStream;

  final _currentApplicationsStream = BehaviorSubject<List<Application>>();
  BehaviorSubject<List<Application>> get currentApplicationsStream =>
      _currentApplicationsStream;

  final _currentEnvironmentStream = BehaviorSubject<String?>();
  BehaviorSubject<String?> get currentEnvironmentStream =>
      _currentEnvironmentStream;

  final _featureGroupUpdateStream =
      BehaviorSubject<List<FeatureGroupUpdateFeature>>();

  Future<void> _getCurrentPortfolioApplications(
      List<Application> appList) async {
    _currentApplicationsStream.add(appList);
  }

  getCurrentFeatureGroups() async {
    if (mrClient.currentAid != null) {
      var featureGroupsList = await featureGroupServiceApi
          .listFeatureGroups(mrClient.currentAid!, environmentId: currentEnvId);
      _featureGroupsStream.add(featureGroupsList.featureGroups);
    }
  }

  createFeatureGroup(String name, String? description) async {
    if (currentEnvId != null) {
      FeatureGroupCreate fgc = FeatureGroupCreate(
          name: name,
          description: description,
          environmentId: currentEnvId!,
          features: []);
      var currentAppId = mrClient.currentAid;
      if (currentAppId != null) {
        FeatureGroupListGroup group =
            await featureGroupServiceApi.createFeatureGroup(currentAppId, fgc);
        List<FeatureGroupListGroup> featureGroupList =
            _featureGroupsStream.value;
        featureGroupList.add(group);
        _featureGroupsStream.add(featureGroupList);
      }
    }
  }

  resetDataStreamsOnAppIdChange(String? id) {
    _currentEnvironmentStream.add(null);
    _featureGroupsStream.add([]);
  }

  @override
  void dispose() {
    _currentApplicationsListListener.cancel();
    _currentApplicationListener.cancel();
    _featureGroupsStream.close();
    _currentEnvironmentStream.close();
    _currentApplicationsStream.close();
    _featureGroupUpdateStream.close();
  }

  @override
  ManagementRepositoryClientBloc get mrClient => _mrClient;

  updateFeatureGroup(FeatureGroupListGroup featureGroupListGroup,
      {String? name,
      String? description,
      List<FeatureGroupUpdateFeature>? features,
      List<FeatureGroupStrategy>? strategies}) async {
    FeatureGroupUpdate fgc = FeatureGroupUpdate(
        name: name,
        description: description,
        version: featureGroupListGroup.version,
        features: features,
        strategies: strategies);
    var currentAppId = mrClient.currentAid;
    if (currentAppId != null) {
      await featureGroupServiceApi.updateFeatureGroup(
          currentAppId, featureGroupListGroup.id, fgc);
      getCurrentFeatureGroups();
    }
  }
}
