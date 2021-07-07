import 'dart:async';

import 'package:open_admin_app/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

class ManageServiceAccountsBloc implements Bloc {
  String? portfolioId;
  final ManagementRepositoryClientBloc mrClient;
  late ServiceAccountServiceApi _serviceAccountServiceApi;

  final _applicationsSubject = BehaviorSubject<Portfolio>();
  Stream<Portfolio> get applicationsList => _applicationsSubject.stream;
  Stream<List<ServiceAccount>> get serviceAccountsList =>
      _serviceAccountSearchResultSource.stream;
  final _serviceAccountSearchResultSource =
      BehaviorSubject<List<ServiceAccount>>();
  late StreamSubscription<String?> _currentPidSubscription;

  ManageServiceAccountsBloc(this.portfolioId, this.mrClient) {
    _serviceAccountServiceApi = ServiceAccountServiceApi(mrClient.apiClient);
    // lets get this party started
    _currentPidSubscription = mrClient.streamValley.currentPortfolioIdStream
        .listen(addServiceAccountsToStream);
  }

  Future<void> addServiceAccountsToStream(String? portfolio) async {
    portfolioId = portfolio;
    if (portfolioId != null && mrClient.userIsCurrentPortfolioAdmin) {
      final serviceAccounts = await _serviceAccountServiceApi
          .searchServiceAccountsInPortfolio(portfolioId!,
              includePermissions: true)
          .catchError((e, s) {
        mrClient.dialogError(e, s);
      });
      if (!_serviceAccountSearchResultSource.isClosed) {
        _serviceAccountSearchResultSource.add(serviceAccounts);
      }

      // we need to fill up a list of applications down to environments
      // ignore: unawaited_futures
      mrClient.portfolioServiceApi
          .getPortfolio(portfolioId!,
              includeApplications: true, includeEnvironments: true)
          .then((portfolio) {
        portfolio.applications.sort((a1, a2) => a1.name.compareTo(a2.name));
        _applicationsSubject.add(portfolio);
      });
    }
  }

  Future<bool> deleteServiceAccount(String sid) async {
    final result = await _serviceAccountServiceApi
        .deleteServiceAccount(sid)
        .catchError((e, s) {
      mrClient.dialogError(e, s);
    });
    await addServiceAccountsToStream(portfolioId);
    await mrClient.streamValley.getCurrentPortfolioServiceAccounts(force: true);
    return result;
  }

  Future<void> updateServiceAccount(ServiceAccount serviceAccount,
      String updatedServiceAccountName, String updatedDescription) async {
    serviceAccount.name = updatedServiceAccountName;
    serviceAccount.description = updatedDescription;
    return _serviceAccountServiceApi
        .updateServiceAccount(serviceAccount.id!, serviceAccount)
        .then((onSuccess) {
      addServiceAccountsToStream(portfolioId);
    }).catchError((e, s) {
      mrClient.dialogError(e, s);
    });
  }

  Future<void> createServiceAccount(
      String serviceAccountName, String description) async {
    if (portfolioId != null) {
      final serviceAccount =
          ServiceAccount(name: serviceAccountName, description: description);
      await _serviceAccountServiceApi
          .createServiceAccountInPortfolio(portfolioId!, serviceAccount)
          .then((onSuccess) {
        addServiceAccountsToStream(mrClient.getCurrentPid());
      }).catchError((e, s) {
        mrClient.dialogError(e, s);
      });
      await mrClient.streamValley
          .getCurrentPortfolioServiceAccounts(force: true);
    }
  }

  @override
  void dispose() {
    _serviceAccountSearchResultSource.close();
    _currentPidSubscription.cancel();
  }
}
