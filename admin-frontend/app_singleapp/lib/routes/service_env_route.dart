import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/mr_client_aware.dart';
import 'package:app_singleapp/widgets/common/application_drop_down.dart';
import 'package:app_singleapp/widgets/common/decorations/fh_page_divider.dart';
import 'package:app_singleapp/widgets/common/fh_header.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

class _ServiceAccountEnvironments {
  final List<Environment> environments;
  final List<ServiceAccount> serviceAccounts;

  _ServiceAccountEnvironments(this.environments, this.serviceAccounts);
}

class ServiceAccountEnvBloc implements Bloc, ManagementRepositoryAwareBloc {
  final ManagementRepositoryClientBloc _mrClient;
  final _serviceAccountSource = BehaviorSubject<_ServiceAccountEnvironments>();
  ServiceAccountServiceApi _serviceAccountServiceApi;

  ServiceAccountEnvBloc(this._mrClient) {
    _serviceAccountServiceApi = ServiceAccountServiceApi(_mrClient.apiClient);
    _mrClient.streamValley.currentApplicationEnvironmentsStream
        .listen(_envUpdate);
  }

  void _envUpdate(List<Environment> envs) async {
    if (envs == null || envs.isEmpty) {
      _serviceAccountSource.add(
          _ServiceAccountEnvironments(<Environment>[], <ServiceAccount>[]));
    } else {
      final serviceAccounts = await _serviceAccountServiceApi
          .searchServiceAccountsInPortfolio(_mrClient.currentPortfolio.id,
              includePermissions: true)
          .catchError(_mrClient.dialogError);

      _serviceAccountSource
          .add(_ServiceAccountEnvironments(envs, serviceAccounts));
    }
  }

  @override
  ManagementRepositoryClientBloc get mrClient => _mrClient;

  Stream<_ServiceAccountEnvironments> get serviceAccountStream =>
      _serviceAccountSource.stream;

  @override
  void dispose() {}

  void setApplicationId(String appId) {}
}

class ServiceAccountEnvRoute extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ServiceAccountEnvBloc>(context);
    return Container(
        padding: const EdgeInsets.fromLTRB(0, 8, 0, 0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            StreamBuilder<List<Application>>(
                stream: bloc
                    .mrClient.streamValley.currentPortfolioApplicationsStream,
                builder: (context, snapshot) {
                  if (snapshot.hasData && snapshot.data.isNotEmpty) {
                    return Container(
                        padding: EdgeInsets.only(left: 8, bottom: 8),
                        child: ApplicationDropDown(
                            applications: snapshot.data, bloc: bloc));
                  } else {
                    bloc.setApplicationId(bloc.mrClient.currentAid);
                    return Container(
                        padding: EdgeInsets.only(left: 8, top: 15),
                        child: Text('No applications found!'));
                  }
                }),
            Container(
              padding: EdgeInsets.only(bottom: 10),
              child: FHHeader(
                title: 'Service Accounts by Environment',
                children: <Widget>[],
              ),
            ),
            FHPageDivider(),
            StreamBuilder<_ServiceAccountEnvironments>(
                stream: bloc.serviceAccountStream,
                builder: (context, envSnapshot) {
                  if (!envSnapshot.hasData) {
                    return SizedBox.shrink();
                  }

                  if (envSnapshot.data.serviceAccounts.isEmpty) {
                    return Text('No service accounts');
                  }

                  return _ServiceAccountDisplayWidget(
                      serviceAccounts: envSnapshot.data);
                }),
          ],
        ));
  }
}

class _ServiceAccountDisplayWidget extends StatelessWidget {
  final _ServiceAccountEnvironments serviceAccounts;

  const _ServiceAccountDisplayWidget({Key key, this.serviceAccounts})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
        shrinkWrap: true,
        itemCount: serviceAccounts.environments.length,
        itemBuilder: (context, index) {
          final env = serviceAccounts.environments[index];
          return Row(
            children: [
              Flexible(
                flex: 1,
                child: Text(env.name),
              ),
              Flexible(
                  flex: 3,
                  child: Column(
                    children: [
                      for (var sa in serviceAccounts.serviceAccounts)
                        Row(
                          children: [
                            Text(sa.name),
                            _ServiceAccountPermissionWidget(env: env, sa: sa)
                          ],
                        )
                    ],
                  ))
            ],
          );
        });
  }
}

class _ServiceAccountPermissionWidget extends StatelessWidget {
  final Environment env;
  final ServiceAccount sa;

  const _ServiceAccountPermissionWidget({Key key, this.env, this.sa})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final perms = sa.permissions
        .firstWhere((p) => p.environmentId == env.id,
            orElse: () =>
                ServiceAccountPermission()..permissions = <RoleType>[])
        .permissions;

    return Text(perms
        .map((p) => RoleTypeTypeTransformer.toJson(p).toString())
        .join(', '));
  }
}
