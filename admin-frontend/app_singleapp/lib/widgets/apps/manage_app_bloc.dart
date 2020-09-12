import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/mr_client_aware.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:pedantic/pedantic.dart';
import 'package:rxdart/rxdart.dart';

enum ManageAppPageState { loadingState, initialState }

class ManageAppBloc implements Bloc, ManagementRepositoryAwareBloc {
  String applicationId;
  Application application;
  Portfolio portfolio;
  final ManagementRepositoryClientBloc _mrClient;
  ApplicationServiceApi _appServiceApi;
  EnvironmentServiceApi _environmentServiceApi;
  PortfolioServiceApi _portfolioServiceApi;
  GroupServiceApi _groupServiceApi;
  ServiceAccountServiceApi _serviceAccountServiceApi;
  List<Environment> environmentsList;
  StreamSubscription<String> _currentAppId;
  String _selectedGroupId;

  ManageAppBloc(this._mrClient) : assert(_mrClient != null) {
    _appServiceApi = ApplicationServiceApi(_mrClient.apiClient);
    _environmentServiceApi = EnvironmentServiceApi(_mrClient.apiClient);
    _portfolioServiceApi = PortfolioServiceApi(_mrClient.apiClient);
    _groupServiceApi = GroupServiceApi(_mrClient.apiClient);
    _serviceAccountServiceApi = ServiceAccountServiceApi(_mrClient.apiClient);
    _pageStateBS.add(ManageAppPageState.loadingState);
    _currentAppId =
        _mrClient.streamValley.currentAppIdStream.listen(setApplicationId);
  }

  @override
  ManagementRepositoryClientBloc get mrClient => _mrClient;

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

  final _groupWithRolesPS = BehaviorSubject<Group>();

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
        .catchError(_mrClient.dialogError);
    environmentsList = application.environments;
    if (!_environmentBS.isClosed) {
      _environmentBS.add(environmentsList);
    }
    portfolio = await _portfolioServiceApi
        .getPortfolio(application.portfolioId)
        .catchError(_mrClient.dialogError);
    if (!_pageStateBS.isClosed) {
      _pageStateBS.add(ManageAppPageState.initialState);
    }
  }

  String get selectedGroup => _selectedGroupId;
  set selectedGroup(String groupId) {
    _selectedGroupId = groupId;
    getGroupRoles(_selectedGroupId);
  }

  Future<void> _fetchGroups() async {
    application = await _appServiceApi
        .getApplication(applicationId)
        .catchError(_mrClient.dialogError);

    portfolio = await _portfolioServiceApi
        .getPortfolio(application.portfolioId, includeGroups: true)
        .catchError(_mrClient.dialogError);
    if (!_groupsBS.isClosed) {
      _groupsBS.add(portfolio.groups);

      if (!portfolio.groups.map((e) => e.id).contains(_selectedGroupId)) {
        if (portfolio.groups.isEmpty) {
          selectedGroup = null;
        } else {
          if (portfolio.groups[0].id != _selectedGroupId) {
            selectedGroup = portfolio.groups[0].id;
          }
        }
      }
    }
  }

  void _fetchServiceAccounts() async {
    application = await _appServiceApi
        .getApplication(applicationId)
        .catchError(_mrClient.dialogError);

    if (_mrClient.userIsCurrentPortfolioAdmin) {
      final serviceAccounts = await _serviceAccountServiceApi
          .searchServiceAccountsInPortfolio(
              application.portfolioId ?? _mrClient.currentPortfolio.id,
              includePermissions: true)
          .catchError(_mrClient.dialogError);
      if (!_serviceAccountsBS.isClosed) {
        if (serviceAccounts.isNotEmpty) {
          _currentServiceAccountIdSource.add(null);
          // ignore: unawaited_futures
          selectServiceAccount(serviceAccounts[0].id);
        }
        _serviceAccountsBS.add(serviceAccounts);
      }
    }
  }

  Future<void> getGroupRoles(String groupId) async {
    if (groupId == null) {
      _groupWithRolesPS.add(null);
    } else {
      final group =
          await _groupServiceApi.getGroup(groupId, includeGroupRoles: true);

      _groupWithRolesPS.add(group);
    }
  }

  void resetGroup(Group group) {
    getGroupRoles(group.id);
  }

  Future<void> selectServiceAccount(String said) async {
    if (_mrClient.userIsCurrentPortfolioAdmin) {
      await _serviceAccountServiceApi
          .callGet(said, includePermissions: true)
          .then((sa) {
        _currentServiceAccountIdSource.add(sa.id);
        if (!_serviceAccountPS.isClosed) {
          _serviceAccountPS.add(sa);
        }
      }).catchError(_mrClient.dialogError);
    }
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
        .catchError(_mrClient.dialogError);
    _refreshEnvironments();
    return updatedGroup;
  }

  void _refreshEnvironments() async {
    var list = await _mrClient.streamValley.getCurrentApplicationEnvironments();
    _environmentBS.add(list);
  }

  Future<ServiceAccount> updateServiceAccountPermissions(
      String sid, ServiceAccount serviceAccount) async {
    final updatedServiceAccount = await _serviceAccountServiceApi
        .update(
          sid,
          serviceAccount,
          includePermissions: true,
        )
        .catchError(_mrClient.dialogError);
    unawaited(_mrClient.streamValley.getEnvironmentServiceAccountPermissions());
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
        .catchError(_mrClient.dialogError);
    if (success) {
      environmentsList.remove(toRemove);
      // await updateEnvs(applicationId, environmentsList);
      _refreshEnvironments();
      return true;
    }
    return false;
  }

  void updateEnvs(String appId, List<Environment> envs) async {
    environmentsList = await _environmentServiceApi
        .environmentOrdering(appId, envs)
        .catchError(_mrClient.dialogError);
    _environmentBS.add(environmentsList);
  }

  void updateEnv(Environment env, String name) async {
    env.name = name;
    await _environmentServiceApi
        .updateEnvironment(env.id, env)
        .catchError(_mrClient.dialogError);
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
//    updateEnvs(applicationId, environmentsList);
    // ignore: unawaited_futures
    _mrClient.streamValley
        .getCurrentApplicationEnvironments()
        .then((envs) => _environmentBS.add(envs));
  }
}
