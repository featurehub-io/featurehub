import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

class ManageServiceAccountsBloc implements Bloc {
  String portfolioId;
  final ManagementRepositoryClientBloc mrClient;
  ServiceAccountServiceApi _serviceAccountServiceApi;

  final _applicationsSubject = BehaviorSubject<Portfolio>();
  Stream<Portfolio> get applicationsList => _applicationsSubject.stream;
  Stream<List<ServiceAccount>> get serviceAccountsList =>
      _serviceAccountSearchResultSource.stream;
  final _serviceAccountSearchResultSource =
      BehaviorSubject<List<ServiceAccount>>();
  StreamSubscription<String> _currentPidSubscription;

  ManageServiceAccountsBloc(this.portfolioId, this.mrClient)
      : assert(mrClient != null) {
    _serviceAccountServiceApi = ServiceAccountServiceApi(mrClient.apiClient);
    // lets get this party started
    _currentPidSubscription = mrClient.streamValley.currentPortfolioIdStream
        .listen(addServiceAccountsToStream);
  }

  void addServiceAccountsToStream(String portfolio) async {
    portfolioId = portfolio;
    if (portfolioId != null && mrClient.userIsCurrentPortfolioAdmin) {
      final serviceAccounts = await _serviceAccountServiceApi
          .searchServiceAccountsInPortfolio(portfolioId,
              includePermissions: true)
          .catchError(mrClient.dialogError);
      if (!_serviceAccountSearchResultSource.isClosed) {
        _serviceAccountSearchResultSource.add(serviceAccounts);
      }

      // we need to fill up a list of applications down to environments
      // ignore: unawaited_futures
      mrClient.portfolioServiceApi
          .getPortfolio(portfolioId,
              includeApplications: true, includeEnvironments: true)
          .then((portfolio) {
        portfolio.applications.sort((a1, a2) => a1.name.compareTo(a2.name));
        _applicationsSubject.add(portfolio);
      });
    }
  }

  Future<bool> deleteServiceAccount(String sid) async {
    final result = await _serviceAccountServiceApi
        .delete(sid)
        .catchError(mrClient.dialogError);
    await addServiceAccountsToStream(portfolioId);
    await mrClient.streamValley.getCurrentPortfolioServiceAccounts();
    return result;
  }

  Future<void> updateServiceAccount(ServiceAccount serviceAccount,
      String updatedServiceAccountName, String updatedDescription) async {
    serviceAccount.name = updatedServiceAccountName;
    serviceAccount.description = updatedDescription;
    return _serviceAccountServiceApi
        .update(serviceAccount.id, serviceAccount)
        .then((onSuccess) {
      addServiceAccountsToStream(portfolioId);
    }).catchError(mrClient.dialogError);
  }

  Future<void> createServiceAccount(
      String serviceAccountName, String description) async {
    final serviceAccount = ServiceAccount();
    serviceAccount.name = serviceAccountName;
    serviceAccount.description = description;
    await _serviceAccountServiceApi
        .createServiceAccountInPortfolio(portfolioId, serviceAccount)
        .then((onSuccess) {
      addServiceAccountsToStream(mrClient.getCurrentPid());
    }).catchError(mrClient.dialogError);
    await mrClient.streamValley.getCurrentPortfolioServiceAccounts();
  }

  @override
  void dispose() {
    _serviceAccountSearchResultSource.close();
    _currentPidSubscription.cancel();
  }
}
