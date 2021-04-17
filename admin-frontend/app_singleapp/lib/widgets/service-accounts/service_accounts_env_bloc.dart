import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/mr_client_aware.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

class ServiceAccountEnvironments {
  final List<Environment> environments;
  final List<ServiceAccount> serviceAccounts;

  ServiceAccountEnvironments(this.environments, this.serviceAccounts);
}

class ServiceAccountEnvBloc implements Bloc, ManagementRepositoryAwareBloc {
  final ManagementRepositoryClientBloc _mrClient;
  final _serviceAccountSource = BehaviorSubject<ServiceAccountEnvironments>();
  ServiceAccountServiceApi _serviceAccountServiceApi;
  StreamSubscription<List<Environment>> envListener;
  bool firstCall = true;

  ServiceAccountEnvBloc(this._mrClient) {
    _serviceAccountServiceApi = ServiceAccountServiceApi(_mrClient.apiClient);
    envListener = _mrClient.streamValley.currentApplicationEnvironmentsStream
        .listen(_envUpdate);
//    if (!_mrClient.userIsFeatureAdminOfCurrentApplication) {
    // ignore: unawaited_futures
    _mrClient.streamValley.getCurrentApplicationEnvironments();
//    }
  }

  void _envUpdate(List<Environment> envs) async {
    if (envs == null || envs.isEmpty) {
      if (firstCall) {
        firstCall = false;
        // if we aren't an admin, we won't have called this, so lets call it now
        if (!_mrClient.userIsFeatureAdminOfCurrentApplication) {
          // ignore: unawaited_futures
          _mrClient.streamValley.getCurrentApplicationEnvironments();
        }
      }
      _serviceAccountSource
          .add(ServiceAccountEnvironments(<Environment>[], <ServiceAccount>[]));
    } else {
      final serviceAccounts = await _serviceAccountServiceApi
          .searchServiceAccountsInPortfolio(_mrClient.currentPortfolio.id,
              applicationId: envs[0].applicationId,
              includePermissions: true,
              includeSdkUrls: true)
          .catchError((e, s) {
        _mrClient.dialogError(e, s);
      });

      _serviceAccountSource
          .add(ServiceAccountEnvironments(envs, serviceAccounts));
    }
  }

  @override
  ManagementRepositoryClientBloc get mrClient => _mrClient;

  Stream<ServiceAccountEnvironments> get serviceAccountStream =>
      _serviceAccountSource.stream;

  @override
  void dispose() {
    envListener.cancel();
  }

  void setApplicationId(String appId) {}
}
