import 'dart:async';

import 'package:collection/collection.dart';
import 'package:logging/logging.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/person_state.dart';
import 'package:rxdart/rxdart.dart';

class ReleasedPortfolio {
  final Portfolio portfolio;
  final bool currentPortfolioOrSuperAdmin;

  @override
  String toString() {
    return 'ReleasedPortfolio{portfolio: $portfolio, currentPortfolioOrSuperAdmin: $currentPortfolioOrSuperAdmin}';
  }

  bool isNull() {
    return this == nullPortfolio;
  }

  ReleasedPortfolio(
      {required this.portfolio, required this.currentPortfolioOrSuperAdmin});
}

class ReleasedApplication {
  final Application application;

  @override
  String toString() {
    return 'ReleasedApplication{application: $application}';
  }

  bool isNull() {
    return this == nullApplication;
  }

  ReleasedApplication({required this.application});
}

typedef FindApplicationsFunc = Future<List<Application>> Function(
    String portfolioId);

final _log = Logger('stream-valley');

final ReleasedPortfolio nullPortfolio = ReleasedPortfolio(
    portfolio: Portfolio(name: 'null-portfolio'),
    currentPortfolioOrSuperAdmin: false);

final ReleasedApplication nullApplication =
    ReleasedApplication(application: Application(name: 'null-app'));

class StreamValley {
  late ManagementRepositoryClientBloc mrClient;
  final PersonState personState;
  late AuthServiceApi authServiceApi;
  late PortfolioServiceApi portfolioServiceApi;
  late ServiceAccountServiceApi serviceAccountServiceApi;
  late EnvironmentServiceApi environmentServiceApi;
  late FeatureServiceApi featureServiceApi;
  late ApplicationServiceApi applicationServiceApi;

  late StreamSubscription<ReleasedPortfolio?>
      currentPortfolioAdminOrSuperAdminSubscription;
  late StreamSubscription<Portfolio?> currentPortfolioSubscription;

  bool _isCurrentPortfolioAdminOrSuperAdmin = false;

  final _portfoliosSource = BehaviorSubject<List<Portfolio>>();
  final _currentPortfolioSource =
      BehaviorSubject<ReleasedPortfolio>.seeded(nullPortfolio);
  final _routeCheckPortfolioSource = BehaviorSubject<Portfolio?>();

  final _currentAppSource = BehaviorSubject.seeded(nullApplication);
  String? get currentAppId => _currentAppSource.value!.application.id;
  Stream<String?> get currentAppIdStream => _currentAppSource.stream
      .map((app) => app.isNull() ? null : app.application.id);
  final _currentPortfolioApplicationsSource =
      BehaviorSubject<List<Application>>.seeded([]);
  final _currentPortfolioGroupsSource = BehaviorSubject<List<Group>>.seeded([]);
  final _currentApplicationEnvironmentsSource =
      BehaviorSubject<List<Environment>>.seeded([]);
  final _currentApplicationFeaturesSource =
      BehaviorSubject<List<Feature>>.seeded([]);
  final _currentEnvironmentServiceAccountSource =
      BehaviorSubject<List<ServiceAccount>>.seeded([]);

  Stream<Portfolio?> get routeCheckPortfolioStream =>
      _routeCheckPortfolioSource;
  Stream<List<Portfolio>> get portfolioListStream => _portfoliosSource.stream;
  Stream<ReleasedPortfolio> get currentPortfolioStream =>
      _currentPortfolioSource.stream;
  ReleasedPortfolio get currentPortfolio => _currentPortfolioSource.value!;

  String? get currentPortfolioId => currentPortfolio.portfolio.id;

  final _globalRefresherSource = BehaviorSubject<String?>();

  Stream<String?> get globalRefresherStream => _globalRefresherSource.stream;

  StreamValley(this.personState) {
    personState.personStream.listen((person) async {
      // print("streamvalley got $person");
      if (personState.isLoggedIn) {
        await loadPortfolios();
      }
    });

    currentPortfolioStream.listen((portfolioUpdate) {
      _isCurrentPortfolioAdminOrSuperAdmin =
          portfolioUpdate.currentPortfolioOrSuperAdmin;

      if (portfolioUpdate != nullPortfolio) {
        getCurrentPortfolioApplications();

        // the portfolio has changed and the app isn't in the portfolio
        if (_currentAppSource.hasValue &&
            !portfolioUpdate.portfolio.applications
                .any((app) => app.id == currentAppId)) {
          if (portfolioUpdate.portfolio.applications.isEmpty) {
            currentAppId = null;
          } else {
            currentAppId = portfolioUpdate.portfolio.applications.first.id;
          }
        }
      }

      if (_isCurrentPortfolioAdminOrSuperAdmin) {
        getCurrentPortfolioGroups();
        getCurrentPortfolioServiceAccounts();
      } else {
        _currentPortfolioGroupsSource.add([]);
        currentPortfolioServiceAccounts = [];
        _lastPortfolioIdServiceAccountChecked = null;
        _lastPortfolioIdGroupChecked = null;
      }
    });
  }

  get userIsCurrentPortfolioAdmin =>
      personState.userIsPortfolioAdmin(currentPortfolioId);

  bool get hasCurrentPortfolio => currentPortfolio != nullPortfolio;

  set apiClient(ManagementRepositoryClientBloc mrClient) {
    this.mrClient = mrClient;
    // print("reset permissions client");
    authServiceApi = AuthServiceApi(mrClient.apiClient);
    portfolioServiceApi = PortfolioServiceApi(mrClient.apiClient);
    serviceAccountServiceApi = ServiceAccountServiceApi(mrClient.apiClient);
    environmentServiceApi = EnvironmentServiceApi(mrClient.apiClient);
    featureServiceApi = FeatureServiceApi(mrClient.apiClient);
    applicationServiceApi = ApplicationServiceApi(mrClient.apiClient);
  }

  void dispose() {
    currentPortfolioSubscription.cancel();
    currentPortfolioAdminOrSuperAdminSubscription.cancel();
  }

  void _refreshApplicationIdChanged() {
    if (!_currentAppSource.value!.isNull()) {
      if (_currentApplicationFeaturesSource.hasListener) {
        getCurrentApplicationFeatures();
      }

      if (_currentApplicationEnvironmentsSource.hasListener) {
        getCurrentApplicationEnvironments();
      }
    }
  }

  set currentPortfolioId(String? value) {
    _log.fine('Attempting to set portfolio at $value');
    if (value == null) {
      _log.fine('Portfolio request was null, storing null.');
      _currentPortfolioSource.add(nullPortfolio);
      _routeCheckPortfolioSource.add(null); // no portfolio
    } else {
      Portfolio? found =
          _portfoliosSource.value?.firstWhereOrNull((p) => p.id == value);
      if (found == null) {
        _log.fine("attempting to swap to portfolio that doesnt exist $value");
      } else if (_currentPortfolioSource.value?.portfolio.id != value) {
        _log.fine('Accepted portfolio id change, triggering');
        _currentPortfolioSource.add(ReleasedPortfolio(
            portfolio: found,
            currentPortfolioOrSuperAdmin:
                personState.isPersonSuperUserOrPortfolioAdmin(found.id)));
        _routeCheckPortfolioSource.add(found);
      }
    }
  }

  Stream<String?> get currentPortfolioIdStream =>
      _currentPortfolioSource.stream.map((p) => p.portfolio.id);

  Stream<ReleasedApplication> get currentAppStream => _currentAppSource.stream;

  ReleasedApplication get currentApp => _currentAppSource.value!;

  set currentAppId(String? value) {
    if (value != _currentAppSource.value!.application.id) {
      if (value == null) {
        _currentAppSource.add(nullApplication);
      } else {
        applicationServiceApi
            .getApplication(value, includeEnvironments: true)
            .then((app) {
          _currentAppSource.add(ReleasedApplication(application: app));
          _refreshApplicationIdChanged();
        }).catchError((e, s) {
          mrClient.dialogError(e, s);
        });
      }
    }
  }

  Stream<List<Application>> get currentPortfolioApplicationsStream =>
      _currentPortfolioApplicationsSource.stream;

  set currentPortfolioApplications(List<Application> value) {
    _currentPortfolioApplicationsSource.add(value);
  }

  Stream<List<Group>> get currentPortfolioGroupsStream =>
      _currentPortfolioGroupsSource.stream;

  final _currentPortfolioServiceAccountsSource =
      BehaviorSubject<List<ServiceAccount>>();

  Stream<List<ServiceAccount>> get currentPortfolioServiceAccountsStream =>
      _currentPortfolioServiceAccountsSource.stream;

  set currentPortfolioServiceAccounts(List<ServiceAccount> value) {
    _currentPortfolioServiceAccountsSource.add(value);
  }

  Stream<List<Environment>> get currentApplicationEnvironmentsStream =>
      _currentApplicationEnvironmentsSource;

  set currentApplicationEnvironments(List<Environment> value) {
    _currentApplicationEnvironmentsSource.add(value);
  }

  Stream<List<Feature>> get currentApplicationFeaturesStream =>
      _currentApplicationFeaturesSource;

  set currentApplicationFeatures(List<Feature> value) {
    _currentApplicationFeaturesSource.add(value);
  }

  Stream<List<ServiceAccount>> get currentEnvironmentServiceAccountStream =>
      _currentEnvironmentServiceAccountSource;

  set currentEnvironmentServiceAccount(List<ServiceAccount> value) {
    _currentEnvironmentServiceAccountSource.add(value);
  }

  bool _includeEnvironmentsInApplicationRequest = false;

  set includeEnvironmentsInApplicationRequest(bool include) {
    // swapping from false to true
    if (_includeEnvironmentsInApplicationRequest != include && include) {
      _includeEnvironmentsInApplicationRequest = include;
      getCurrentPortfolioApplications();
    }
  }

  Future<void> getCurrentPortfolioApplications(
      {FindApplicationsFunc? findApp}) async {
    List<Application> appList;
    if (currentPortfolioId != null) {
      if (findApp != null) {
        appList = await findApp(currentPortfolioId!).catchError((e, s) {
          mrClient.dialogError(e, s);
        });
      } else {
        appList = await applicationServiceApi
            .findApplications(currentPortfolioId!,
                order: SortOrder.DESC,
                includeEnvironments: true,
                includeFeatures: _includeEnvironmentsInApplicationRequest)
            .catchError((e, s) {
          mrClient.dialogError(e, s);
        });
      }

      currentPortfolioApplications = appList;

      // we refreshed the apps, is the current app id in the list anymore? if not
      // we may have changed portfolios or deleted the app
      if (!appList.map((a) => a.id).contains(currentAppId)) {
        if (appList.isNotEmpty) {
          currentAppId = appList[0].id;
        } else {
          currentAppId = null;
        }
      }

      if (currentAppId != null) {
        if (mrClient.rocketOpened) {
          // we need more info
          await getCurrentApplicationEnvironments();
        } else {
          final app = appList.firstWhereOrNull((app) => app.id == currentAppId);
          if (app != null) {
            currentApplicationEnvironments = app.environments;
          } else {
            currentApplicationEnvironments = [];
          }

        }
      }
    } else {
      currentPortfolioApplications = [];
    }
  }

  String? _lastPortfolioIdGroupChecked;
  Future<List<Group>> getCurrentPortfolioGroups({bool force = false}) async {
    if (currentPortfolioId != _lastPortfolioIdGroupChecked ||
        _lastPortfolioIdGroupChecked == null ||
        force) {
      _lastPortfolioIdGroupChecked = currentPortfolioId;
      // print("current portfolio id is $currentPortfolioId");
      if (currentPortfolioId != null) {
        await portfolioServiceApi
            .getPortfolio(currentPortfolioId!, includeGroups: true)
            .then((portfolio) =>
                _currentPortfolioGroupsSource.add(portfolio.groups))
            .catchError((e, s) {
          mrClient.dialogError(e, s);
        });
      } else {
        _currentPortfolioGroupsSource.add([]);
      }
    }

    return _currentPortfolioGroupsSource.value!;
  }

  String? _lastPortfolioIdServiceAccountChecked;
  Future<void> getCurrentPortfolioServiceAccounts({bool force = false}) async {
    if (currentPortfolioId != _lastPortfolioIdServiceAccountChecked ||
        _lastPortfolioIdServiceAccountChecked == null ||
        force) {
      _lastPortfolioIdServiceAccountChecked = currentPortfolioId;

      if (currentPortfolioId != null) {
        await serviceAccountServiceApi
            .searchServiceAccountsInPortfolio(currentPortfolioId!)
            .then((accounts) => currentPortfolioServiceAccounts = accounts)
            .catchError((e, s) {
          mrClient.dialogError(e, s);
        });
      } else {
        currentPortfolioServiceAccounts = [];
      }
    }
  }

  Future<List<Environment>> getCurrentApplicationEnvironments() async {
    var envList = <Environment>[];

    if (!_currentAppSource.value!.isNull()) {
      envList = await environmentServiceApi
          .findEnvironments(currentAppId!, includeAcls: true)
          .catchError((e, s) {
        mrClient.dialogError(e, s);
      });
    }

    currentApplicationEnvironments = envList;
    return envList;
  }

  Future<void> getCurrentApplicationFeatures() async {
    if (!currentApp.isNull()) {
      final featureList = await featureServiceApi
          .getAllFeaturesForApplication(currentApp.application.id!)
          .catchError((e, s) {
        mrClient.dialogError(e, s);
      });
      currentApplicationFeatures = featureList;
    } else {
      currentApplicationFeatures = [];
    }
  }

  Future<void> getEnvironmentServiceAccountPermissions() async {
    if (!currentApp.isNull() && !currentPortfolio.isNull()) {
      final saList = await serviceAccountServiceApi
          .searchServiceAccountsInPortfolio(currentPortfolioId!,
              includePermissions: true, applicationId: currentAppId!)
          .catchError((e, s) {
        mrClient.dialogError(e, s);
      });
      currentEnvironmentServiceAccount = saList;
    } else {
      currentEnvironmentServiceAccount = [];
    }
  }

  // this is triggered when a person is loaded (i.e. changed)
  // or when new portfolios are created
  Future<List<Portfolio>> loadPortfolios() async {
    final portfolios = await portfolioServiceApi.findPortfolios(
        includeApplications: true, order: SortOrder.ASC);

    // print("loaded portfolios are $portfolios");

    _portfoliosSource.add(portfolios);

    if (portfolios.isEmpty) {
      currentPortfolioId = null;
    } else if (!portfolios.any((p) => currentPortfolio.portfolio.id == p.id)) {
      // current portfolio no access?
      currentPortfolioId = portfolios.first.id;
    } else if (_currentPortfolioSource.hasValue &&
        !personState.userHasPortfolioPermission(currentPortfolioId)) {
      _currentPortfolioSource.add(nullPortfolio);
    } else if (_currentAppSource.hasValue &&
        !personState.userHasApplicationPermission(currentAppId)) {
      currentAppId = null;
    }

    return portfolios;
  }

  bool containsPid(String? pid) {
    if (pid == null) return false;
    return _portfoliosSource.value?.any((p) => p.id == pid) ?? false;
  }

  /*
   * This is used to allow forcing global things, e.g. portfolios, users and
   * admin service accounts.
   */
  void triggerGlobalRefresh(String? id) {
    _globalRefresherSource.add(id);
  }
}
