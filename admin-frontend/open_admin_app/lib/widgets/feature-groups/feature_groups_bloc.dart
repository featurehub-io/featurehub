import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/widgets.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/mr_client_aware.dart';
import 'package:open_admin_app/fhos_logger.dart';
import 'package:rxdart/rxdart.dart';

class FeatureGroupsBloc implements Bloc, ManagementRepositoryAwareBloc {
  late FeatureGroupServiceApi featureGroupServiceApi;
  late ApplicationServiceApi applicationServiceApi;
  final ManagementRepositoryClientBloc _mrClient;

  late StreamSubscription<List<Application>> _currentApplicationsListListener;
  late StreamSubscription<String?> _currentAppIdListener;
  late StreamSubscription<String?> _currentEnvIdListener;

  String? currentEnvId;
  String? appId;
  ApplicationPermissions? userRoles;
  bool triggerEnvironmentListener = true;

  FeatureGroupsBloc(this._mrClient) {
    _currentApplicationsListListener = mrClient
        .streamValley.currentPortfolioApplicationsStream
        .listen(_getCurrentPortfolioApplications);
    mrClient.streamValley.includeEnvironmentsInApplicationRequest = true;
    featureGroupServiceApi = FeatureGroupServiceApi(_mrClient.apiClient);
    applicationServiceApi = ApplicationServiceApi(_mrClient.apiClient);
    mrClient.streamValley.includeEnvironmentsInApplicationRequest = true;

    // this tells the mrClient to run any callback code after the page has finished loading
    WidgetsBinding.instance
        .addPostFrameCallback((_) => mrClient.processLandingActions());
    _currentAppIdListener =
        _mrClient.streamValley.currentAppIdStream.listen(_refreshInitialData);

    _currentEnvIdListener =
        _mrClient.streamValley.currentEnvIdStream.listen(_updateEnvId);
  }

  final _featureGroupsStream =
      BehaviorSubject<List<FeatureGroupListGroup>>.seeded([]);
  BehaviorSubject<List<FeatureGroupListGroup>> get featureGroupsStream =>
      _featureGroupsStream;

  final _envRoleTypeStream = BehaviorSubject<List<RoleType>>.seeded([]);
  BehaviorSubject<List<RoleType>> get envRoleTypeStream => _envRoleTypeStream;

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

  getCurrentFeatureGroups(String? envId, String? appId) async {
    if (appId != null) {
      var featureGroupsList = await featureGroupServiceApi
          .listFeatureGroups(appId, environmentId: currentEnvId, max: 1000);
      _featureGroupsStream.add(featureGroupsList.featureGroups);
    }
  }

  _updateEnvId(String? envId) {
    _currentEnvironmentStream.add(envId);
  }

  _refreshInitialData(String? appId) async {
    this.appId = appId;

    if (appId != null) {
      // check if the environment list has been refreshed. As there were no listeners before we were created,
      // its likely it has out-of-sync
      if (triggerEnvironmentListener) {
        mrClient.streamValley.getCurrentApplicationEnvironments();
        triggerEnvironmentListener = false;
      }
      await getPermissions(appId);
      if (appId != mrClient.streamValley.currentApp.application.id) {
        // only refresh if appId has changed
        resetDataStreamsOnAppIdChange();
      }
    }
  }

  getPermissions(String? appId, {String? envId}) async {
    if (appId != null) {
      userRoles = await applicationServiceApi.applicationPermissions(appId);
      if (userRoles != null) {
        var envRoles = userRoles!.environments
            .firstWhereOrNull((env) => env.id == (envId ?? currentEnvId));

        if (envRoles != null) {
          _envRoleTypeStream.add(envRoles.roles);
        }
      }
    } else {
      userRoles = null;
    }
  }

  createFeatureGroup(String name, String? description) async {
    if (currentEnvId != null) {
      FeatureGroupCreate fgc = FeatureGroupCreate(
          name: name,
          description: description ?? name,
          environmentId: currentEnvId!,
          features: []);
      if (appId != null) {
        FeatureGroupListGroup group =
            await featureGroupServiceApi.createFeatureGroup(appId!, fgc);
        List<FeatureGroupListGroup> featureGroupList =
            _featureGroupsStream.value;
        featureGroupList.add(group);
        _featureGroupsStream.add(featureGroupList);
      }
    }
  }

  resetDataStreamsOnAppIdChange() {
    _currentEnvironmentStream.add(null);
    _featureGroupsStream.add([]);
  }

  @override
  void dispose() {
    _currentApplicationsListListener.cancel();
    _currentAppIdListener.cancel();
    _currentEnvIdListener.cancel();
    _featureGroupsStream.close();
    _currentEnvironmentStream.close();
    _currentApplicationsStream.close();
    _featureGroupUpdateStream.close();
    _envRoleTypeStream.close();
  }

  @override
  ManagementRepositoryClientBloc get mrClient => _mrClient;

  updateFeatureGroup(FeatureGroupListGroup featureGroupListGroup,
      {String? name,
      String? description,
      List<FeatureGroupUpdateFeature>? features,
      List<GroupRolloutStrategy>? strategies}) async {
    FeatureGroupUpdate fgc = FeatureGroupUpdate(
        id: featureGroupListGroup.id,
        name: name,
        description: description,
        version: featureGroupListGroup.version,
        features: features,
        strategies: strategies);
    fhosLogger.fine('Updating feature group with $fgc');
    if (appId != null) {
      await featureGroupServiceApi.updateFeatureGroup(appId!, fgc);
      getCurrentFeatureGroups(currentEnvId, appId);
    }
  }

  deleteFeatureGroup(String id) async {
    if (appId != null) {
      await featureGroupServiceApi.deleteFeatureGroup(appId!, id);
      List<FeatureGroupListGroup> featureGroupList = _featureGroupsStream.value;
      featureGroupList.removeWhere((group) => group.id == id);
      _featureGroupsStream.add(featureGroupList);
    }
  }
}
