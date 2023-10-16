import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
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
  late StreamSubscription<String?> _currentApplicationListener;

  String? currentEnvId;
  ApplicationPermissions? userRoles;

  FeatureGroupsBloc(this._mrClient) {
    _currentApplicationsListListener = mrClient
        .streamValley.currentPortfolioApplicationsStream
        .listen(_getCurrentPortfolioApplications);
    _currentApplicationListener = mrClient.streamValley.currentAppIdStream
        .listen(resetDataStreamsOnAppIdChange);
    mrClient.streamValley.includeEnvironmentsInApplicationRequest = true;
    featureGroupServiceApi = FeatureGroupServiceApi(_mrClient.apiClient);
    applicationServiceApi = ApplicationServiceApi(_mrClient.apiClient);
    mrClient.streamValley.includeEnvironmentsInApplicationRequest = true;

    // this tells the mrClient to run any callback code after the page has finished loading
    WidgetsBinding.instance
        .addPostFrameCallback((_) => mrClient.processLandingActions());
    _currentAppIdListener =
        _mrClient.streamValley.currentAppIdStream.listen(_getPermissions);
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

  getCurrentFeatureGroups() async {
    if (mrClient.currentAid != null) {
      var featureGroupsList = await featureGroupServiceApi
          .listFeatureGroups(mrClient.currentAid!, environmentId: currentEnvId);
      _featureGroupsStream.add(featureGroupsList.featureGroups);
      if (userRoles != null) {
        var envRoles =
            userRoles!.environments.firstWhere((env) => env.id == currentEnvId);
        _envRoleTypeStream.add(envRoles.roles);
      }
    }
  }

  _getPermissions(String? appId) async {
    if (appId != null) {
      userRoles = await applicationServiceApi
          .applicationPermissions(mrClient.currentAid!);
    }
  }

  createFeatureGroup(String name, String? description) async {
    if (currentEnvId != null) {
      FeatureGroupCreate fgc = FeatureGroupCreate(
          name: name,
          description: description ?? name,
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
    _currentAppIdListener.cancel();
    _currentApplicationListener.cancel();
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
    print("update fg $features -> $strategies");
    FeatureGroupUpdate fgc = FeatureGroupUpdate(
        id: featureGroupListGroup.id,
        name: name,
        description: description,
        version: featureGroupListGroup.version,
        features: features,
        strategies: strategies);
    fhosLogger.fine('Updating feature group with ${fgc}');
    var currentAppId = mrClient.currentAid;
    if (currentAppId != null) {
      await featureGroupServiceApi.updateFeatureGroup(
          currentAppId, fgc);
      getCurrentFeatureGroups();
    }
  }

  deleteFeatureGroup(String id) async {
    var currentAppId = mrClient.currentAid;
    if (currentAppId != null) {
      await featureGroupServiceApi.deleteFeatureGroup(currentAppId, id);
      List<FeatureGroupListGroup> featureGroupList = _featureGroupsStream.value;
      featureGroupList.removeWhere((group) => group.id == id);
      _featureGroupsStream.add(featureGroupList);
    }
  }
}
