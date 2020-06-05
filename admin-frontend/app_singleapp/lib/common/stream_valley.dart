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

    currentPortfolioAdminOrSuperAdminSubscription = personState.currentPortfolioAdminOrSuperAdminStream.listen((val) {
      _isCurrentPortfolioAdminOrSuperAdmin = val;
      _refreshApplicationIdChanged();
    });
  }

  void _refreshApplicationIdChanged() {
    if (_isCurrentPortfolioAdminOrSuperAdmin && _currentAppIdStream.value != null) {
      getCurrentApplicationFeatures();
      getCurrentApplicationEnvironments();
      getEnvironmentServiceAccountPermissions();
    }
  }


  final _currentPortfolioIdStream = BehaviorSubject<String>();

  set currentPortfolioId(String value) {
    _currentPortfolioIdStream.add(value);
  }

  Stream<String> get currentPortfolioIdStream => _currentPortfolioIdStream.stream;

  final _currentAppIdStream = BehaviorSubject<String>();
  Stream<String> get currentAppIdStream => _currentAppIdStream.stream;


  set currentAppId(String value) {
    _currentAppIdStream.add(value);
    _refreshApplicationIdChanged();
  }

  final _currentPortfolioApplicationsStream = BehaviorSubject<List<Application>>();

  get currentPortfolioApplicationsStream => _currentPortfolioApplicationsStream;

  set currentPortfolioApplicationsStream(List<Application> value) {
    _currentPortfolioApplicationsStream.add(value);
  }

  final _currentPortfolioGroupsStream = BehaviorSubject<List<Group>>();

  get currentPortfolioGroupsStream => _currentPortfolioGroupsStream;

  set currentPortfolioGroupsStream(List<Group> value) {
    _currentPortfolioGroupsStream.add(value);
  }

  final _currentPortfolioServiceAccountsStream = BehaviorSubject<List<ServiceAccount>>();

  get currentPortfolioServiceAccountsStream =>
      _currentPortfolioServiceAccountsStream;

  set currentPortfolioServiceAccountsStream(List<ServiceAccount> value) {
    _currentPortfolioServiceAccountsStream.add(value);
  }

  final _currentApplicationEnvironmentsStream = BehaviorSubject<List<Environment>>();

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

  final _currentEnvironmentServiceAccountStream = BehaviorSubject<List<ServiceAccount>>();

  get currentEnvironmentServiceAccountStream =>
      _currentEnvironmentServiceAccountStream;

  set currentEnvironmentServiceAccountStream(List<ServiceAccount> value) {
    _currentEnvironmentServiceAccountStream.add(value);
  }


  Future<void> getCurrentPortfolioApplications() async {
    List<Application> appList;
    if (_currentPortfolioIdStream.value != null) {
      appList = await applicationServiceApi.findApplications(
          _currentPortfolioIdStream.value,
          order: SortOrder.DESC).catchError(mrClient.dialogError);
      currentPortfolioApplicationsStream = appList;
    } else {
      currentPortfolioApplicationsStream = [];
    }

  }

  Future<void> getCurrentPortfolioGroups() async {
    if (_currentPortfolioIdStream.value != null) {
      Portfolio portfolio = await portfolioServiceApi.getPortfolio(_currentPortfolioIdStream.value,
          includeGroups: true).catchError(mrClient.dialogError);
      currentPortfolioGroupsStream = portfolio.groups;
    } else {
      currentPortfolioGroupsStream = [];
    }
  }

  Future<void> getCurrentPortfolioServiceAccounts() async {
    if (_currentPortfolioIdStream.value != null) {
      List<ServiceAccount> accounts = await serviceAccountServiceApi
          .searchServiceAccountsInPortfolio(_currentPortfolioIdStream.value).catchError(mrClient.dialogError);
      currentPortfolioServiceAccountsStream = accounts;
    }
    else {
      currentPortfolioServiceAccountsStream = [];
    }
  }

  Future<void> getCurrentApplicationEnvironments() async {
    if (_currentAppIdStream.value != null) {
      List<Environment> envList =
      await environmentServiceApi.findEnvironments(_currentAppIdStream.value, includeAcls: true).catchError(mrClient.dialogError);
      currentApplicationEnvironmentsStream = envList;
    }
    else {
      currentApplicationEnvironmentsStream = [];
    }
  }

  Future<void> getCurrentApplicationFeatures() async {
    if (_currentAppIdStream.value != null) {
      List<Feature> featureList =
      await featureServiceApi.getAllFeaturesForApplication(_currentAppIdStream.value).catchError(mrClient.dialogError);
      currentApplicationFeaturesStream = featureList;
    }
    else {
      currentApplicationFeaturesStream = [];
    }
  }

  Future<void> getEnvironmentServiceAccountPermissions() async {
    if (_currentAppIdStream.value != null) {
      List<ServiceAccount> saList = await serviceAccountServiceApi
          .searchServiceAccountsInPortfolio(_currentPortfolioIdStream.value,
          includePermissions: true, applicationId: _currentAppIdStream.value)
          .catchError(mrClient.dialogError);
      currentEnvironmentServiceAccountStream = saList;
    }
    else {
      currentEnvironmentServiceAccountStream = [];
    }
  }

}
