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
      _isCurrentPortfolioAdminOrSuperAdmin = val;
      _refreshApplicationIdChanged();
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
  Stream<List<Portfolio>> get portfolioListStream => _portfoliosSource.stream;
  Stream<Portfolio> get currentPortfolioStream =>
      _currentPortfolioSource.stream;
  Portfolio get currentPortfolio => _currentPortfolioSource.value;

  final _currentPortfolioIdSource = BehaviorSubject<String>();

  String get currentPortfolioId => _currentPortfolioIdSource.value;

  set currentPortfolioId(String value) {
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

  Stream<String> get currentPortfolioIdStream =>
      _currentPortfolioIdSource.stream;

  final _currentAppIdSource = BehaviorSubject<String>();
  Stream<String> get currentAppIdStream => _currentAppIdSource.stream;

  String get currentAppId => _currentAppIdSource.value;

  set currentAppId(String value) {
    _currentAppIdSource.add(value);
    _refreshApplicationIdChanged();
  }

  final _currentPortfolioApplicationsStream =
      BehaviorSubject<List<Application>>();

  get currentPortfolioApplicationsStream => _currentPortfolioApplicationsStream;

  set currentPortfolioApplicationsStream(List<Application> value) {
    _currentPortfolioApplicationsStream.add(value);
  }

  final _currentPortfolioGroupsStream = BehaviorSubject<List<Group>>();

  get currentPortfolioGroupsStream => _currentPortfolioGroupsStream;

  set currentPortfolioGroupsStream(List<Group> value) {
    _currentPortfolioGroupsStream.add(value);
  }

  final _currentPortfolioServiceAccountsStream =
      BehaviorSubject<List<ServiceAccount>>();

  get currentPortfolioServiceAccountsStream =>
      _currentPortfolioServiceAccountsStream;

  set currentPortfolioServiceAccountsStream(List<ServiceAccount> value) {
    _currentPortfolioServiceAccountsStream.add(value);
  }

  final _currentApplicationEnvironmentsStream =
      BehaviorSubject<List<Environment>>();

  get currentApplicationEnvironmentsStream =>
      _currentApplicationEnvironmentsStream;

  set currentApplicationEnvironmentsStream(List<Environment> value) {
    _currentApplicationEnvironmentsStream.add(value);
  }

  final _currentApplicationFeaturesStream = BehaviorSubject<List<Feature>>();

  get currentApplicationFeaturesStream => _currentApplicationFeaturesStream;

  set currentApplicationFeaturesStream(List<Feature> value) {
    _currentApplicationFeaturesStream.add(value);
  }

  final _currentEnvironmentServiceAccountStream =
      BehaviorSubject<List<ServiceAccount>>();

  get currentEnvironmentServiceAccountStream =>
      _currentEnvironmentServiceAccountStream;

  set currentEnvironmentServiceAccountStream(List<ServiceAccount> value) {
    _currentEnvironmentServiceAccountStream.add(value);
  }

  Future<void> getCurrentPortfolioApplications() async {
    List<Application> appList;
    if (_currentPortfolioIdSource.value != null) {
      appList = await applicationServiceApi
          .findApplications(_currentPortfolioIdSource.value,
              order: SortOrder.DESC)
          .catchError(mrClient.dialogError);
      currentPortfolioApplicationsStream = appList;

      if (appList.isNotEmpty && appList.length == 1) {
        currentAppId = appList[0].id;
      }
    } else {
      currentPortfolioApplicationsStream = [];
    }
  }

  Future<void> getCurrentPortfolioGroups() async {
    if (_currentPortfolioIdSource.value != null) {
      final portfolio = await portfolioServiceApi
          .getPortfolio(_currentPortfolioIdSource.value, includeGroups: true)
          .catchError(mrClient.dialogError);
      currentPortfolioGroupsStream = portfolio.groups;
    } else {
      currentPortfolioGroupsStream = [];
    }
  }

  Future<void> getCurrentPortfolioServiceAccounts() async {
    if (_currentPortfolioIdSource.value != null) {
      final accounts = await serviceAccountServiceApi
          .searchServiceAccountsInPortfolio(_currentPortfolioIdSource.value)
          .catchError(mrClient.dialogError);
      currentPortfolioServiceAccountsStream = accounts;
    } else {
      currentPortfolioServiceAccountsStream = [];
    }
  }

  Future<void> getCurrentApplicationEnvironments() async {
    if (_currentAppIdSource.value != null) {
      final envList = await environmentServiceApi
          .findEnvironments(_currentAppIdSource.value, includeAcls: true)
          .catchError(mrClient.dialogError);
      currentApplicationEnvironmentsStream = envList;
    } else {
      currentApplicationEnvironmentsStream = [];
    }
  }

  Future<void> getCurrentApplicationFeatures() async {
    if (_currentAppIdSource.value != null) {
      final featureList = await featureServiceApi
          .getAllFeaturesForApplication(_currentAppIdSource.value)
          .catchError(mrClient.dialogError);
      currentApplicationFeaturesStream = featureList;
    } else {
      currentApplicationFeaturesStream = [];
    }
  }

  Future<void> getEnvironmentServiceAccountPermissions() async {
    if (_currentAppIdSource.value != null) {
      final saList = await serviceAccountServiceApi
          .searchServiceAccountsInPortfolio(_currentPortfolioIdSource.value,
              includePermissions: true,
              applicationId: _currentAppIdSource.value)
          .catchError(mrClient.dialogError);
      currentEnvironmentServiceAccountStream = saList;
    } else {
      currentEnvironmentServiceAccountStream = [];
    }
  }

  Future<List<Portfolio>> loadPortfolios() async {
    final portfolios = await portfolioServiceApi.findPortfolios(
        includeApplications: true, order: SortOrder.ASC);

    _portfoliosSource.add(portfolios);

    return portfolios;
  }
}
