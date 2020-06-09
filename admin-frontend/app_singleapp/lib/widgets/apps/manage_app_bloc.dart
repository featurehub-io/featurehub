import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:pedantic/pedantic.dart';
import 'package:rxdart/rxdart.dart';

enum ManageAppPageState { loadingState, initialState }

class ManageAppBloc implements Bloc {
  String appId;
  Application application;
  Portfolio portfolio;
  ManagementRepositoryClientBloc mrClient;
  ApplicationServiceApi _appServiceApi;
  EnvironmentServiceApi _environmentServiceApi;
  PortfolioServiceApi _portfolioServiceApi;
  GroupServiceApi _groupServiceApi;
  ServiceAccountServiceApi _serviceAccountServiceApi;
  List<Environment> environmentsList;

  ManageAppBloc(this.mrClient) : assert(mrClient != null) {
    _appServiceApi = ApplicationServiceApi(mrClient.apiClient);
    _environmentServiceApi = EnvironmentServiceApi(mrClient.apiClient);
    _portfolioServiceApi = PortfolioServiceApi(mrClient.apiClient);
    _groupServiceApi = GroupServiceApi(mrClient.apiClient);
    _serviceAccountServiceApi = ServiceAccountServiceApi(mrClient.apiClient);
    _pageStateBS.add(ManageAppPageState.loadingState);
  }

  @override
  void dispose() {
    _environmentBS.close();
    _pageStateBS.close();
    _groupWithRolesPS.close();
    _groupsBS.close();
    _serviceAccountsBS.close();
    _serviceAccountPS.close();
  }

  final _environmentBS = BehaviorSubject<List<Environment>>();

  Stream<List<Environment>> get environmentsStream => _environmentBS.stream;

  final _groupsBS = BehaviorSubject<List<Group>>();

  Stream<List<Group>> get groupsStream => _groupsBS.stream;

  final _serviceAccountsBS = BehaviorSubject<List<ServiceAccount>>();

  Stream<List<ServiceAccount>> get serviceAccountsStream =>
      _serviceAccountsBS.stream;

  final _serviceAccountPS = PublishSubject<ServiceAccount>();

  Stream<ServiceAccount> get serviceAccountStream => _serviceAccountPS.stream;

  final _groupWithRolesPS = PublishSubject<Group>();

  Stream<Group> get groupRoleStream => _groupWithRolesPS.stream;

  final _pageStateBS = BehaviorSubject<ManageAppPageState>();

  Stream<ManageAppPageState> get pageStateStream => _pageStateBS.stream;

  setApplicationId(String applicationId) {
    _pageStateBS.add(ManageAppPageState.loadingState);
    this.appId = applicationId;
    if (applicationId != null) {
      _fetchGroups();
      _fetchEnvironments();
      _fetchServiceAccounts();
    }
  }

  _fetchEnvironments() async {
    this.application = await _appServiceApi
        .getApplication(appId, includeEnvironments: true)
        .catchError(mrClient.dialogError);
    this.environmentsList = application.environments;
    if (!_environmentBS.isClosed) {
      _environmentBS.add(environmentsList);
    }
    this.portfolio = await _portfolioServiceApi
        .getPortfolio(this.application.portfolioId)
        .catchError(mrClient.dialogError);
    if (!_pageStateBS.isClosed) {
      _pageStateBS.add(ManageAppPageState.initialState);
    }
  }

  Future<void> _fetchGroups() async {
    this.application = await _appServiceApi
        .getApplication(appId)
        .catchError(mrClient.dialogError);
    this.portfolio = await _portfolioServiceApi
        .getPortfolio(application.portfolioId, includeGroups: true)
        .catchError(mrClient.dialogError);
    if (!_groupsBS.isClosed) {
      _groupsBS.add(this.portfolio.groups);
    }
  }

  _fetchServiceAccounts() async {
    this.application = await _appServiceApi
        .getApplication(appId)
        .catchError(mrClient.dialogError);
    List<ServiceAccount> serviceAccounts = await _serviceAccountServiceApi
        .searchServiceAccountsInPortfolio(application.portfolioId,
            includePermissions: true)
        .catchError(mrClient.dialogError);
    if (!_serviceAccountsBS.isClosed) {
      _serviceAccountsBS.add(serviceAccounts);
    }
  }

  Future<void> getGroupRoles(String groupId) async {
    var group =
        await _groupServiceApi.getGroup(groupId, includeGroupRoles: true);
    if (!_groupWithRolesPS.isClosed) {
      _groupWithRolesPS.add(group);
    }
  }

  resetGroup(Group group) {
    getGroupRoles(group.id);
  }

  Future<void> selectServiceAccount(String said) async {
    ServiceAccount sa = await _serviceAccountServiceApi
        .callGet(said, includePermissions: true)
        .catchError(mrClient.dialogError);
    if (!_serviceAccountPS.isClosed) {
      _serviceAccountPS.add(sa);
    }
  }

  Future<Group> updateGroupWithEnvironmentRoles(gid, Group group) async {
    // make sure members are null or they all get removed
    group.members = null;
    Group updatedGroup = await _groupServiceApi
        .updateGroup(gid, group,
            includeGroupRoles: true,
            includeMembers: false,
            updateMembers: false,
            updateApplicationGroupRoles: true,
            updateEnvironmentGroupRoles: true)
        .catchError(mrClient.dialogError);
    unawaited(mrClient.streamValley.getCurrentApplicationEnvironments());
    return updatedGroup;
  }

  Future<ServiceAccount> updateServiceAccountPermissions(
      String sid, ServiceAccount serviceAccount) async {
    ServiceAccount updatedServiceAccount = await _serviceAccountServiceApi
        .update(
          sid,
          serviceAccount,
          includePermissions: true,
        )
        .catchError(mrClient.dialogError);
    unawaited(mrClient.streamValley.getEnvironmentServiceAccountPermissions());
    return updatedServiceAccount;
  }

  Future<bool> deleteEnv(String eid) async {
    Environment toRemove = environmentsList.firstWhere((env) => env.id == eid);
    Environment toUpdate = environmentsList.firstWhere(
        (env) => env.priorEnvironmentId == toRemove.id,
        orElse: () => null);
    if (toUpdate != null) {
      toUpdate.priorEnvironmentId = toRemove.priorEnvironmentId;
    }
    bool success = false;
    success = await _environmentServiceApi
        .deleteEnvironment(eid, includeAcls: true, includeFeatures: true)
        .catchError(mrClient.dialogError);
    if (success) {
      environmentsList.remove(toRemove);
      await updateEnvs(appId, environmentsList);
      unawaited(mrClient.streamValley.getCurrentApplicationEnvironments());
      return true;
    }
    return false;
  }

  updateEnvs(String appId, List<Environment> envs) async {
    environmentsList = await _environmentServiceApi
        .environmentOrdering(appId, envs)
        .catchError(mrClient.dialogError);
    _environmentBS.add(environmentsList);
  }

  updateEnv(Environment env, String name) async {
    env.name = name;
    await _environmentServiceApi
        .updateEnvironment(env.id, env)
        .catchError(mrClient.dialogError);
    _fetchEnvironments();
  }

  Future<void> createEnv(String name, bool _isProduction) async {
    Environment toUpdate = environmentsList.firstWhere(
        (env) => env.priorEnvironmentId == null,
        orElse: () => null);
    Environment env = Environment()
      ..name = name
      ..production = _isProduction;
    env = await _environmentServiceApi.createEnvironment(appId, env);
    if (toUpdate != null) {
      toUpdate.priorEnvironmentId = env.id;
    }
    environmentsList.add(env);
    updateEnvs(appId, environmentsList);
    unawaited(mrClient.streamValley.getCurrentApplicationEnvironments());
  }

  Future<bool> deleteApp(String appId) async {
    bool success = false;
    try {
      success = await _appServiceApi.deleteApplication(appId);
      await mrClient.streamValley.getCurrentPortfolioApplications();
      application = null;
      mrClient.setCurrentAid(null);
      setApplicationId(null);
    } catch (e, s) {
      mrClient.dialogError(e, s);
    }
    ;
    return success;
  }

  Future<void> updateApplication(Application application, String updatedAppName,
      String updateDescription) async {
    application.name = updatedAppName;
    application.description = updateDescription;
    return _appServiceApi
        .updateApplication(application.id, application)
        .then((onSuccess) {
      mrClient.streamValley.getCurrentPortfolioApplications();
    });
  }

  Future<void> createApplication(
      String applicationName, String appDescription) async {
    Application application = Application();
    application.name = applicationName;
    application.description = appDescription;
    Application newApp = await _appServiceApi.createApplication(
        mrClient.currentPid, application);
    await mrClient.streamValley.getCurrentPortfolioApplications();
    mrClient.setCurrentAid(newApp.id);
  }
}
