import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/common/person_state.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

class StreamValley {
  final ManagementRepositoryClientBloc mrClient;
  final PersonState personState;
  AuthServiceApi authServiceApi;
  PortfolioServiceApi portfolioServiceApi;
  ServiceAccountServiceApi serviceAccountServiceApi;
  EnvironmentServiceApi environmentServiceApi;
  FeatureServiceApi featureServiceApi;
  ApplicationServiceApi applicationServiceApi;

  StreamSubscription<bool> currentPortfolioAdminOrSuperAdminSubscription;

  bool _isCurrentPortfolioAdminOrSuperAdmin = false;

  StreamValley(this.mrClient, this.personState) {
    authServiceApi = AuthServiceApi(mrClient.apiClient);
    portfolioServiceApi = PortfolioServiceApi(mrClient.apiClient);
    serviceAccountServiceApi = ServiceAccountServiceApi(mrClient.apiClient);
    environmentServiceApi = EnvironmentServiceApi(mrClient.apiClient);
    featureServiceApi = FeatureServiceApi(mrClient.apiClient);
    applicationServiceApi = ApplicationServiceApi(mrClient.apiClient);

    currentPortfolioAdminOrSuperAdminSubscription =
        personState.currentPortfolioAdminOrSuperAdminStream.listen((val) {
      final oldVal = _isCurrentPortfolioAdminOrSuperAdmin;
      _isCurrentPortfolioAdminOrSuperAdmin = val;
      _refreshApplicationIdChanged();
      if (oldVal != _isCurrentPortfolioAdminOrSuperAdmin &&
          _isCurrentPortfolioAdminOrSuperAdmin) {
        getCurrentPortfolioGroups();
        getCurrentPortfolioServiceAccounts();
      }
    });
  }

  void _refreshApplicationIdChanged() {
    if (_isCurrentPortfolioAdminOrSuperAdmin &&
        _currentAppIdSource.value != null) {
      getCurrentApplicationFeatures();
      getCurrentApplicationEnvironments();
      getEnvironmentServiceAccountPermissions();
    }
  }

  final _portfoliosSource = BehaviorSubject<List<Portfolio>>();
  final _currentPortfolioSource = BehaviorSubject<Portfolio>();
  final _currentAppIdSource = BehaviorSubject<String>();
  final _currentPortfolioApplicationsSource =
      BehaviorSubject<List<Application>>();
  final _currentPortfolioIdSource = BehaviorSubject<String>();
  final _currentPortfolioGroupsStream = BehaviorSubject<List<Group>>();
  final _currentApplicationEnvironmentsSource =
      BehaviorSubject<List<Environment>>();
  final _currentApplicationFeaturesSource = BehaviorSubject<List<Feature>>();
  final _currentEnvironmentServiceAccountSource =
      BehaviorSubject<List<ServiceAccount>>();

  Stream<List<Portfolio>> get portfolioListStream => _portfoliosSource.stream;
  Stream<Portfolio> get currentPortfolioStream =>
      _currentPortfolioSource.stream;
  Portfolio get currentPortfolio => _currentPortfolioSource.value;

  String get currentPortfolioId => _currentPortfolioIdSource.value;

  set currentPortfolioId(String value) {
    if (_currentPortfolioIdSource.value != value) {
      currentAppId = null;

      // figure out which one we are
      _currentPortfolioSource.add(
          _portfoliosSource.value.firstWhere((element) => element.id == value));
      _currentPortfolioIdSource.add(value);

      // now load the applications for this portfolio, which may trigger selecting one
      getCurrentPortfolioApplications();

      // if we are an admin, load the groups and service accounts
      if (_isCurrentPortfolioAdminOrSuperAdmin) {
        getCurrentPortfolioGroups();
        getCurrentPortfolioServiceAccounts();
      }
    }
  }

  Stream<String> get currentPortfolioIdStream =>
      _currentPortfolioIdSource.stream;

  Stream<String> get currentAppIdStream => _currentAppIdSource.stream;

  String get currentAppId => _currentAppIdSource.value;

  set currentAppId(String value) {
    _currentAppIdSource.add(value);
    _refreshApplicationIdChanged();
  }

  Stream<List<Application>> get currentPortfolioApplicationsStream =>
      _currentPortfolioApplicationsSource.stream;

  set currentPortfolioApplications(List<Application> value) {
    _currentPortfolioApplicationsSource.add(value);
  }

  Stream<List<Group>> get currentPortfolioGroupsStream =>
      _currentPortfolioGroupsStream.stream;

  set currentPortfolioGroups(List<Group> value) {
    _currentPortfolioGroupsStream.add(value);
  }

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

  Future<void> getCurrentPortfolioApplications() async {
    List<Application> appList;
    if (_currentPortfolioIdSource.value != null) {
      appList = await applicationServiceApi
          .findApplications(_currentPortfolioIdSource.value,
              order: SortOrder.DESC)
          .catchError(mrClient.dialogError);
      currentPortfolioApplications = appList;

      if (appList.isNotEmpty) {
        currentAppId = appList[0].id;
      }
    } else {
      currentPortfolioApplications = [];
    }
  }

  Future<void> getCurrentPortfolioGroups() async {
    if (_currentPortfolioIdSource.value != null) {
      await portfolioServiceApi
          .getPortfolio(_currentPortfolioIdSource.value, includeGroups: true)
          .then((portfolio) => currentPortfolioGroups = portfolio.groups)
          .catchError(mrClient.dialogError);
    } else {
      currentPortfolioGroups = [];
    }
  }

  Future<void> getCurrentPortfolioServiceAccounts() async {
    if (_currentPortfolioIdSource.value != null) {
      await serviceAccountServiceApi
          .searchServiceAccountsInPortfolio(_currentPortfolioIdSource.value)
          .then((accounts) => currentPortfolioServiceAccounts = accounts)
          .catchError(mrClient.dialogError);
    } else {
      currentPortfolioServiceAccounts = [];
    }
  }

  Future<void> getCurrentApplicationEnvironments() async {
    if (_currentAppIdSource.value != null) {
      final envList = await environmentServiceApi
          .findEnvironments(_currentAppIdSource.value, includeAcls: true)
          .catchError(mrClient.dialogError);
      currentApplicationEnvironments = envList;
    } else {
      currentApplicationEnvironments = [];
    }
  }

  Future<void> getCurrentApplicationFeatures() async {
    if (_currentAppIdSource.value != null) {
      final featureList = await featureServiceApi
          .getAllFeaturesForApplication(_currentAppIdSource.value)
          .catchError(mrClient.dialogError);
      currentApplicationFeatures = featureList;
    } else {
      currentApplicationFeatures = [];
    }
  }

  Future<void> getEnvironmentServiceAccountPermissions() async {
    if (_currentAppIdSource.value != null) {
      final saList = await serviceAccountServiceApi
          .searchServiceAccountsInPortfolio(_currentPortfolioIdSource.value,
              includePermissions: true,
              applicationId: _currentAppIdSource.value)
          .catchError(mrClient.dialogError);
      currentEnvironmentServiceAccount = saList;
    } else {
      currentEnvironmentServiceAccount = [];
    }
  }

  Future<List<Portfolio>> loadPortfolios() async {
    final portfolios = await portfolioServiceApi.findPortfolios(
        includeApplications: true, order: SortOrder.ASC);

    _portfoliosSource.add(portfolios);

    return portfolios;
  }
}
