import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:pedantic/pedantic.dart';
import 'package:rxdart/rxdart.dart';

enum ManageAppPageState { loadingState, initialState }

class ManageAppBloc implements Bloc {
  String applicationId;
  Application application;
  Portfolio portfolio;
  ManagementRepositoryClientBloc mrClient;
  ApplicationServiceApi _appServiceApi;
  EnvironmentServiceApi _environmentServiceApi;
  PortfolioServiceApi _portfolioServiceApi;
  GroupServiceApi _groupServiceApi;
  ServiceAccountServiceApi _serviceAccountServiceApi;
  List<Environment> environmentsList;
  StreamSubscription<String> _currentAppId;

  ManageAppBloc(this.mrClient) : assert(mrClient != null) {
    _appServiceApi = ApplicationServiceApi(mrClient.apiClient);
    _environmentServiceApi = EnvironmentServiceApi(mrClient.apiClient);
    _portfolioServiceApi = PortfolioServiceApi(mrClient.apiClient);
    _groupServiceApi = GroupServiceApi(mrClient.apiClient);
    _serviceAccountServiceApi = ServiceAccountServiceApi(mrClient.apiClient);
    _pageStateBS.add(ManageAppPageState.loadingState);
    _currentAppId =
        mrClient.streamValley.currentAppIdStream.listen(setApplicationId);
  }

  @override
  void dispose() {
    _environmentBS.close();
    _pageStateBS.close();
    _groupWithRolesPS.close();
    _groupsBS.close();
    _serviceAccountsBS.close();
    _serviceAccountPS.close();
    _currentAppId.cancel();
  }

  final _environmentBS = BehaviorSubject<List<Environment>>();

  Stream<List<Environment>> get environmentsStream => _environmentBS.stream;

  final _currentServiceAccountIdSource = BehaviorSubject<String>();

  Stream<String> get currentServiceAccountIdStream =>
      _currentServiceAccountIdSource.stream;

  String get currentServiceAccountId => _currentServiceAccountIdSource.value;

  final _groupsBS = BehaviorSubject<List<Group>>();

  Stream<List<Group>> get groupsStream => _groupsBS.stream;

  final _serviceAccountsBS = BehaviorSubject<List<ServiceAccount>>();

  Stream<List<ServiceAccount>> get serviceAccountsStream =>
      _serviceAccountsBS.stream;

  final _serviceAccountPS = BehaviorSubject<ServiceAccount>();

  Stream<ServiceAccount> get serviceAccountStream => _serviceAccountPS.stream;

  final _groupWithRolesPS = PublishSubject<Group>();

  Stream<Group> get groupRoleStream => _groupWithRolesPS.stream;

  final _pageStateBS = BehaviorSubject<ManageAppPageState>();

  Stream<ManageAppPageState> get pageStateStream => _pageStateBS.stream;

  void setApplicationId(String id) {
    _pageStateBS.add(ManageAppPageState.loadingState);
    applicationId = id;
    if (id != null) {
      _fetchGroups();
      _fetchEnvironments();
      _fetchServiceAccounts();
    }
  }

  void _fetchEnvironments() async {
    application = await _appServiceApi
        .getApplication(applicationId, includeEnvironments: true)
        .catchError(mrClient.dialogError);
    environmentsList = application.environments;
    if (!_environmentBS.isClosed) {
      _environmentBS.add(environmentsList);
    }
    portfolio = await _portfolioServiceApi
        .getPortfolio(application.portfolioId)
        .catchError(mrClient.dialogError);
    if (!_pageStateBS.isClosed) {
      _pageStateBS.add(ManageAppPageState.initialState);
    }
  }

  Future<void> _fetchGroups() async {
    application = await _appServiceApi
        .getApplication(applicationId)
        .catchError(mrClient.dialogError);
    portfolio = await _portfolioServiceApi
        .getPortfolio(application.portfolioId, includeGroups: true)
        .catchError(mrClient.dialogError);
    if (!_groupsBS.isClosed) {
      _groupsBS.add(portfolio.groups);
    }
  }

  void _fetchServiceAccounts() async {
    application = await _appServiceApi
        .getApplication(applicationId)
        .catchError(mrClient.dialogError);
    final serviceAccounts = await _serviceAccountServiceApi
        .searchServiceAccountsInPortfolio(application.portfolioId,
            includePermissions: true)
        .catchError(mrClient.dialogError);
    if (!_serviceAccountsBS.isClosed) {
      if (serviceAccounts.isNotEmpty) {
        _currentServiceAccountIdSource.add(null);
        // ignore: unawaited_futures
        selectServiceAccount(serviceAccounts[0].id);
      }
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

  void resetGroup(Group group) {
    getGroupRoles(group.id);
  }

  Future<void> selectServiceAccount(String said) async {
    await _serviceAccountServiceApi
        .callGet(said, includePermissions: true)
        .then((sa) {
      _currentServiceAccountIdSource.add(sa.id);
      if (!_serviceAccountPS.isClosed) {
        _serviceAccountPS.add(sa);
      }
    }).catchError(mrClient.dialogError);
  }

  Future<Group> updateGroupWithEnvironmentRoles(gid, Group group) async {
    // make sure members are null or they all get removed
    group.members = null;
    final updatedGroup = await _groupServiceApi
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
    final updatedServiceAccount = await _serviceAccountServiceApi
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
    final toRemove = environmentsList.firstWhere((env) => env.id == eid);
    final toUpdate = environmentsList.firstWhere(
        (env) => env.priorEnvironmentId == toRemove.id,
        orElse: () => null);
    if (toUpdate != null) {
      toUpdate.priorEnvironmentId = toRemove.priorEnvironmentId;
    }
    final success = await _environmentServiceApi
        .deleteEnvironment(eid, includeAcls: true, includeFeatures: true)
        .catchError(mrClient.dialogError);
    if (success) {
      environmentsList.remove(toRemove);
      await updateEnvs(applicationId, environmentsList);
      unawaited(mrClient.streamValley.getCurrentApplicationEnvironments());
      return true;
    }
    return false;
  }

  void updateEnvs(String appId, List<Environment> envs) async {
    environmentsList = await _environmentServiceApi
        .environmentOrdering(appId, envs)
        .catchError(mrClient.dialogError);
    _environmentBS.add(environmentsList);
  }

  void updateEnv(Environment env, String name) async {
    env.name = name;
    await _environmentServiceApi
        .updateEnvironment(env.id, env)
        .catchError(mrClient.dialogError);
    _fetchEnvironments();
  }

  Future<void> createEnv(String name, bool _isProduction) async {
    final toUpdate = environmentsList.firstWhere(
        (env) => env.priorEnvironmentId == null,
        orElse: () => null);
    final env = await _environmentServiceApi.createEnvironment(
        applicationId,
        Environment()
          ..name = name
          ..production = _isProduction);
    if (toUpdate != null) {
      toUpdate.priorEnvironmentId = env.id;
    }
    environmentsList.add(env);
    updateEnvs(applicationId, environmentsList);
    unawaited(mrClient.streamValley.getCurrentApplicationEnvironments());
  }

  Future<bool> deleteApp(String appId) async {
    var success = false;
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
    final application = Application();
    application.name = applicationName;
    application.description = appDescription;
    final newApp = await _appServiceApi.createApplication(
        mrClient.currentPid, application);
    await mrClient.streamValley.getCurrentPortfolioApplications();
    mrClient.setCurrentAid(newApp.id);
  }
}
