import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/mr_client_aware.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:pedantic/pedantic.dart';
import 'package:rxdart/rxdart.dart';

enum ManageAppPageState { loadingState, initialState }

class ManageAppBloc implements Bloc, ManagementRepositoryAwareBloc {
  final ManagementRepositoryClientBloc _mrClient;
  late ApplicationServiceApi _appServiceApi;
  late EnvironmentServiceApi _environmentServiceApi;
  late PortfolioServiceApi _portfolioServiceApi;
  late GroupServiceApi _groupServiceApi;
  late ServiceAccountServiceApi _serviceAccountServiceApi;
  late StreamSubscription<String?> _currentAppIdSubscription;

  // operational fields
  String? applicationId;
  Application? application;
  Portfolio? portfolio;
  String? _selectedGroupId;
  List<Environment> environmentsList = [];

  ManageAppBloc(this._mrClient) {
    _appServiceApi = ApplicationServiceApi(_mrClient.apiClient);
    _environmentServiceApi = EnvironmentServiceApi(_mrClient.apiClient);
    _portfolioServiceApi = PortfolioServiceApi(_mrClient.apiClient);
    _groupServiceApi = GroupServiceApi(_mrClient.apiClient);
    _serviceAccountServiceApi = ServiceAccountServiceApi(_mrClient.apiClient);
    _pageStateBS.add(ManageAppPageState.loadingState);
    _currentAppIdSubscription =
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
    _currentAppIdSubscription.cancel();
  }

  final _environmentBS = BehaviorSubject<List<Environment>>();

  Stream<List<Environment>> get environmentsStream => _environmentBS.stream;

  final _currentServiceAccountIdSource = BehaviorSubject<String?>();

  Stream<String?> get currentServiceAccountIdStream =>
      _currentServiceAccountIdSource.stream;

  String? get currentServiceAccountId => _currentServiceAccountIdSource.value;

  final _groupsBS = BehaviorSubject<List<Group>>();

  Stream<List<Group>> get groupsStream => _groupsBS.stream;

  final _serviceAccountsBS = BehaviorSubject<List<ServiceAccount>>();

  Stream<List<ServiceAccount>> get serviceAccountsStream =>
      _serviceAccountsBS.stream;

  final _serviceAccountPS = BehaviorSubject<ServiceAccount>();

  Stream<ServiceAccount> get serviceAccountStream => _serviceAccountPS.stream;

  final _groupWithRolesPS = BehaviorSubject<Group?>();

  Stream<Group?> get groupRoleStream => _groupWithRolesPS.stream;

  final _pageStateBS = BehaviorSubject<ManageAppPageState>();

  Stream<ManageAppPageState> get pageStateStream => _pageStateBS.stream;

  void setApplicationId(String? id) {
    _pageStateBS.add(ManageAppPageState.loadingState);
    applicationId = id;
    if (id != null) {
      _fetchGroups();
      _fetchEnvironments();
      _fetchServiceAccounts();
    }
  }

  Future<void> applicationIdChanged() async {
    if (await refreshApplication()) {
      // ignore: unawaited_futures
      _fetchGroups(existingApp: true);
      _fetchEnvironments(existingApp: true);
      _fetchEnvironments(existingApp: true);
    }
  }

  Future<bool> refreshApplication() async {
    if (applicationId == null) {
      return false;
    }

    return await _appServiceApi
        .getApplication(applicationId!, includeEnvironments: true)
        .then((value) {
      application = value;
      return true;
    }).catchError((e, s) {
      _mrClient.dialogError(e, s);
      return false;
    });
  }

  void _fetchEnvironments({existingApp = false}) async {
    if (!existingApp || application == null) {
      if (!(await refreshApplication())) {
        return;
      }
    }

    environmentsList = application!.environments;
    if (!_environmentBS.isClosed) {
      _environmentBS.add(application!.environments);
    }
    portfolio = await _portfolioServiceApi
        .getPortfolio(application!.portfolioId!)
        .catchError((e, s) {
      _mrClient.dialogError(e, s);
    });
    if (!_pageStateBS.isClosed) {
      _pageStateBS.add(ManageAppPageState.initialState);
    }
  }

  String? get selectedGroup => _selectedGroupId;
  set selectedGroup(String? groupId) {
    _selectedGroupId = groupId;
    getGroupRoles(_selectedGroupId);
  }

  Future<void> _fetchGroups({existingApp = false}) async {
    if (!existingApp || application == null) {
      if (!(await refreshApplication())) {
        return;
      }
    }

    portfolio = await _portfolioServiceApi
        .getPortfolio(application!.portfolioId!, includeGroups: true)
        .catchError((e, s) {
      _mrClient.dialogError(e, s);
    });
    if (portfolio != null && !_groupsBS.isClosed) {
      _groupsBS.add(portfolio!.groups);

      if (!portfolio!.groups.map((e) => e.id).contains(_selectedGroupId)) {
        if (portfolio!.groups.isEmpty) {
          selectedGroup = null;
        } else {
          if (portfolio!.groups[0].id != _selectedGroupId) {
            selectedGroup = portfolio!.groups[0].id!;
          }
        }
      }
    }
  }

  Future<void> _fetchServiceAccountsFromApplication(Application app) async {
    if (_mrClient.userIsCurrentPortfolioAdmin) {
      try {
        final serviceAccounts = await _serviceAccountServiceApi
            .searchServiceAccountsInPortfolio(app.portfolioId!,
                includePermissions: true);

        if (!_serviceAccountsBS.isClosed) {
          if (serviceAccounts.isNotEmpty) {
            _currentServiceAccountIdSource.add(null);
            // ignore: unawaited_futures
            selectServiceAccount(serviceAccounts[0].id!);
          }
          _serviceAccountsBS.add(serviceAccounts);
        }
      } catch (e, s) {
        await _mrClient.dialogError(e, s);
      }
    }
  }

  void _fetchServiceAccounts({existingApp = false}) async {
    if (!existingApp || application == null) {
      if (!(await refreshApplication())) {
        return;
      }
    }

    try {
      await _fetchServiceAccountsFromApplication(application!);
    } catch (e, s) {
      await _mrClient.dialogError(e, s);
    }
  }

  Future<void> getGroupRoles(String? groupId) async {
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
      }).catchError((e, s) {
        _mrClient.dialogError(e, s);
      });
    }
  }

  Future<Group> updateGroupWithEnvironmentRoles(gid, Group group) async {
    // make sure members are null or they all get removed
    group.members = [];
    final updatedGroup = await _groupServiceApi
        .updateGroup(gid, group,
            includeGroupRoles: true,
            includeMembers: false,
            updateMembers: false,
            updateApplicationGroupRoles: true,
            updateEnvironmentGroupRoles: true)
        .catchError((e, s) {
      _mrClient.dialogError(e, s);
    });
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
        .catchError((e, s) {
      _mrClient.dialogError(e, s);
    });
    unawaited(_mrClient.streamValley.getEnvironmentServiceAccountPermissions());
    return updatedServiceAccount;
  }

  Future<bool> deleteEnv(String eid) async {
    final toRemove = environmentsList.firstWhere((env) => env.id == eid);
    final toUpdate = environmentsList
        .firstWhereOrNull((env) => env.priorEnvironmentId == toRemove.id);
    if (toUpdate != null) {
      toUpdate.priorEnvironmentId = toRemove.priorEnvironmentId;
    }
    final success = await _environmentServiceApi
        .deleteEnvironment(eid, includeAcls: true, includeFeatures: true)
        .catchError((e, s) {
      _mrClient.dialogError(e, s);
      return false;
    });

    if (success) {
      environmentsList.remove(toRemove);
      // await updateEnvs(applicationId, environmentsList);
      _refreshEnvironments();
      return true;
    }
    return false;
  }

  Future<void> updateEnvs(String appId, List<Environment> envs) async {
    environmentsList = await _environmentServiceApi
        .environmentOrdering(appId, envs)
        .catchError((e, s) {
      _mrClient.dialogError(e, s);
    });
    _environmentBS.add(environmentsList);
  }

  Future<void> updateEnv(Environment env, String name) async {
    env.name = name;
    await _environmentServiceApi
        .updateEnvironment(env.id!, env)
        .catchError((e, s) {
      _mrClient.dialogError(e, s);
    });
    _fetchEnvironments();
  }

  Future<void> createEnv(String name, bool _isProduction) async {
    final toUpdate = environmentsList
        .firstWhereOrNull((env) => env.priorEnvironmentId == null);
    final env = await _environmentServiceApi.createEnvironment(
        applicationId!,
        Environment(
          name: name,
          production: _isProduction,
        ));
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
