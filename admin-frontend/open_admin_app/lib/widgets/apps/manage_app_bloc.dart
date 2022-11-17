import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/mr_client_aware.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:openapi_dart_common/openapi.dart';
import 'package:rxdart/rxdart.dart';

enum ManageAppPageState { loadingState, initialState }

class ApplicationGroupRoles {
  final Group group;
  final String applicationId;

  ApplicationGroupRoles(this.group, this.applicationId);
}

class ManageAppBloc implements Bloc, ManagementRepositoryAwareBloc {
  final ManagementRepositoryClientBloc _mrClient;
  late StreamSubscription<String?> _currentAppIdSubscription;
  late StreamSubscription<Application?>
      _currentApplicationWithEnvironmentSubscription;
  late StreamSubscription<List<Group>> _currentPortfolioGroupsSubscription;
  late StreamSubscription<ReleasedPortfolio> _currentPortfolioSubscription;

  // operational fields
  String? applicationId;
  Portfolio? portfolio;
  String? _selectedGroupId;
  List<Environment> environmentsList = [];

  ApplicationServiceApi get _appServiceApi => _mrClient.applicationServiceApi;
  EnvironmentServiceApi get _environmentServiceApi =>
      _mrClient.environmentServiceApi;
  GroupServiceApi get _groupServiceApi => _mrClient.groupServiceApi;
  ServiceAccountServiceApi get _serviceAccountServiceApi =>
      _mrClient.serviceAccountServiceApi;

  ManageAppBloc(this._mrClient) {
    _pageStateBS.add(ManageAppPageState.loadingState);

    _currentPortfolioSubscription =
        _mrClient.streamValley.currentPortfolioStream.listen(_updatedPortfolio);

    _currentAppIdSubscription =
        _mrClient.streamValley.currentAppIdStream.listen(setApplicationId);

    _currentPortfolioGroupsSubscription = _mrClient
        .streamValley.currentPortfolioGroupsStream
        .listen(_updatePortfolioGroups);

    // we don't listen to the app itself because we need our own version, we
    // listen to our own stream instead
    _currentApplicationWithEnvironmentSubscription =
        applicationWithEnvironments.listen((app) {
      _publishEnvironmentListUpdate(app);
      _updateCurrentlySelectedGroupPermissions();
      _refreshServiceAccountsOnApplicationChange(app);
    });
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
    _applicationWithEnvironmentsBS.close();
    _currentAppIdSubscription.cancel();
    _currentApplicationWithEnvironmentSubscription.cancel();
    _currentPortfolioGroupsSubscription.cancel();
    _currentPortfolioSubscription.cancel();
  }

  final _applicationWithEnvironmentsBS = BehaviorSubject<Application?>();

  Stream<Application?> get applicationWithEnvironments =>
      _applicationWithEnvironmentsBS.stream;

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

  final _groupWithRolesPS = BehaviorSubject<ApplicationGroupRoles?>();

  Stream<ApplicationGroupRoles?> get groupRoleStream =>
      _groupWithRolesPS.stream;

  final _pageStateBS = BehaviorSubject<ManageAppPageState?>();

  Stream<ManageAppPageState?> get pageStateStream => _pageStateBS.stream;

  void setApplicationId(String? id) {
    if (id != applicationId) {
      _pageStateBS.add(ManageAppPageState.loadingState);

      applicationId = id;
      _refreshApplication();
    }
  }

  void _updatedPortfolio(ReleasedPortfolio portfolio) {
    this.portfolio = portfolio.portfolio;
  }

  // the portfolio changed, so update the groups
  void _updatePortfolioGroups(List<Group> groups) {
    if (!_groupsBS.isClosed) {
      _groupsBS.add(groups);
      if (groups.isEmpty) {
        selectedGroup = null;
      } else {
        if (groups[0].id != _selectedGroupId) {
          selectedGroup = groups[0].id!;
        }
      }
    }
  }

  Future<void> _refreshApplication() async {
    if (applicationId == null) {
      return;
    }

    await _appServiceApi
        .getApplication(applicationId!, includeEnvironments: true)
        .then((value) async {
      _applicationWithEnvironmentsBS.add(value);
      if (_pageStateBS.value == ManageAppPageState.loadingState) {
        _pageStateBS.add(ManageAppPageState.initialState);
      }
    }).catchError((e, s) {
      if (!(e is ApiException && e.code == 404)) {
        _mrClient.dialogError(e, s);
      }
      _applicationWithEnvironmentsBS.add(null);
      _pageStateBS.add(ManageAppPageState.loadingState);
    });
  }

  void _publishEnvironmentListUpdate(Application? app) async {
    if (app == null) {
      environmentsList = [];
    } else {
      environmentsList = app.environments;
    }

    if (!_environmentBS.isClosed) {
      _environmentBS.add(environmentsList);
    }
  }

  String? get selectedGroup => _selectedGroupId;

  set selectedGroup(String? groupId) {
    _selectedGroupId = groupId;
    _getGroupRoles(_selectedGroupId);
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

  void _refreshServiceAccountsOnApplicationChange(Application? app) async {
    if (app == null) {
      _serviceAccountsBS.add([]);
    } else {
      try {
        await _fetchServiceAccountsFromApplication(app);
      } catch (e, s) {
        await _mrClient.dialogError(e, s);
      }
    }
  }

  // the application changed, so we need to refresh the group roles
  void _updateCurrentlySelectedGroupPermissions() {
    _getGroupRoles(selectedGroup);
  }

  Future<void> _getGroupRoles(String? groupId) async {
    if (groupId == null ||
        !_mrClient.userIsCurrentPortfolioAdmin ||
        applicationId == null) {
      _groupWithRolesPS.add(null);
    } else {
      try {
        final group = await _groupServiceApi.getGroup(groupId,
            includeGroupRoles: true, byApplicationId: applicationId);

        // the downstream needs to know which application this group is paired with
        // so it knows when to refresh its internal state
        _groupWithRolesPS.add(ApplicationGroupRoles(group, applicationId!));
      } catch (e, s) {
        // print("this group has failed");
        await _mrClient.dialogError(e, s);
        _groupWithRolesPS.add(null);
      }
    }
  }

  void resetGroup(Group group) {
    _getGroupRoles(group.id);
  }

  Future<void> selectServiceAccount(String said) async {
    if (_mrClient.userIsCurrentPortfolioAdmin) {
      await _serviceAccountServiceApi
          .getServiceAccount(said,
              includePermissions: true, byApplicationId: applicationId)
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
            applicationId: applicationId,
            updateApplicationGroupRoles: true,
            updateEnvironmentGroupRoles: true)
        .catchError((e, s) {
      _mrClient.dialogError(e, s);
    });

    // we need to do this to ensure the list of environments has the right set of ACLs
    if (_mrClient.rocketOpened) {
      await _mrClient.streamValley.getCurrentApplicationEnvironments();
    }

    return updatedGroup;
  }

  Future<ServiceAccount> updateServiceAccountPermissions(
      String sid, ServiceAccount serviceAccount) async {
    final updatedServiceAccount = await _serviceAccountServiceApi
        .updateServiceAccount(
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
      // await updateEnvs(applicationId, environmentsList);
      await _refreshApplication();
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
        .then((e) => _refreshApplication())
        .catchError((e, s) {
      _mrClient.dialogError(e, s);
    });
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
