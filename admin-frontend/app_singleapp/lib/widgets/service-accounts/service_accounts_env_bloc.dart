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

  ServiceAccountEnvBloc(this._mrClient) {
    _serviceAccountServiceApi = ServiceAccountServiceApi(_mrClient.apiClient);
    _mrClient.streamValley.currentApplicationEnvironmentsStream
        .listen(_envUpdate);
  }

  void _envUpdate(List<Environment> envs) async {
    if (envs == null || envs.isEmpty) {
      _serviceAccountSource
          .add(ServiceAccountEnvironments(<Environment>[], <ServiceAccount>[]));
    } else {
      final serviceAccounts = await _serviceAccountServiceApi
          .searchServiceAccountsInPortfolio(_mrClient.currentPortfolio.id,
              applicationId: envs[0].applicationId,
              includePermissions: true,
              includeSdkUrls: true)
          .catchError(_mrClient.dialogError);

      _serviceAccountSource
          .add(ServiceAccountEnvironments(envs, serviceAccounts));
    }
  }

  @override
  ManagementRepositoryClientBloc get mrClient => _mrClient;

  Stream<ServiceAccountEnvironments> get serviceAccountStream =>
      _serviceAccountSource.stream;

  @override
  void dispose() {}

  void setApplicationId(String appId) {}
}
