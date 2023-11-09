import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';

import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/widget_creator.dart';
import 'package:open_admin_app/widgets/common/application_drop_down.dart';
import 'package:open_admin_app/widgets/common/copy_to_clipboard_html.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_widget.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/common/fh_underline_button.dart';
import 'package:open_admin_app/widgets/service-accounts/service_accounts_env_bloc.dart';

class ApiKeysRoute extends StatelessWidget {
  const ApiKeysRoute({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ServiceAccountEnvBloc>(context);
    FHAnalytics.sendScreenView("api-keys");
    return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              const Wrap(
                children: [
                  FHHeader(title: 'API Keys', children: [
                    FHExternalLinkWidget(
                      tooltipMessage: "View documentation",
                      link:
                          "https://docs.featurehub.io/featurehub/latest/service-accounts.html#_api_keys",
                      icon: Icon(Icons.arrow_outward_outlined),
                      label: 'API Keys Documentation',
                    )
                  ]),
                ],
              ),
              Padding(
                padding:
                    const EdgeInsets.symmetric(vertical: 16.0, horizontal: 8.0),
                child: Row(
                  children: [
                    StreamBuilder<List<Application>>(
                        stream: bloc.mrClient.streamValley
                            .currentPortfolioApplicationsStream,
                        builder: (context, snapshot) {
                          if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                            return ApplicationDropDown(
                                applications: snapshot.data!, bloc: bloc);
                          } else {
                            return const SizedBox.shrink();
                          }
                        }),
                    StreamBuilder<ReleasedPortfolio?>(
                        stream:
                            bloc.mrClient.streamValley.currentPortfolioStream,
                        builder: (context, snapshot) {
                          if (snapshot.data != null &&
                              (snapshot.data!.currentPortfolioOrSuperAdmin ==
                                  true)) {
                            return Padding(
                              padding: const EdgeInsets.only(left: 8.0),
                              child: FHUnderlineButton(
                                  title: 'Go to service accounts settings',
                                  onPressed: () => {
                                        ManagementRepositoryClientBloc.router
                                            .navigateTo(
                                          context,
                                          '/service-accounts',
                                        )
                                      }),
                            );
                          } else {
                            return const SizedBox.shrink();
                          }
                        }),
                  ],
                ),
              ),
              const SizedBox(
                height: 8.0,
              ),
              const FHPageDivider(),
              const SizedBox(
                height: 16.0,
              ),
              StreamBuilder<ServiceAccountEnvironments>(
                  stream: bloc.serviceAccountEnvironmentsStream,
                  builder: (context, envSnapshot) {
                    if (envSnapshot.connectionState ==
                        ConnectionState.waiting) {
                      return const FHLoadingIndicator();
                    } else if (envSnapshot.connectionState ==
                            ConnectionState.active ||
                        envSnapshot.connectionState == ConnectionState.done) {
                      if (envSnapshot.hasError) {
                        return const FHLoadingError();
                      } else if (envSnapshot.hasData) {
                        if (envSnapshot.data!.serviceAccounts.isEmpty) {
                          return Text('No service accounts available',
                              style: Theme.of(context).textTheme.bodySmall);
                        } else {
                          return _ApiKeysDisplayWidget(
                              serviceAccountEnvs: envSnapshot.data!,
                              bloc: bloc);
                        }
                      }
                    }
                    return const SizedBox.shrink();
                  }),
            ],
          ),
        ]);
  }
}

class _ApiKeysDisplayWidget extends StatelessWidget {
  final ServiceAccountEnvironments serviceAccountEnvs;
  final ServiceAccountEnvBloc bloc;

  const _ApiKeysDisplayWidget(
      {Key? key, required this.serviceAccountEnvs, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    // filter out SA that don't have any permissions

    return Column(
      children: [
        widgetCreator.edgeUrlCopyWidget(bloc.mrClient),
        const Padding(
          padding: EdgeInsets.all(8.0),
          child: Row(
            children: [
              Expanded(
                flex: 3,
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: SelectionArea(
                    child: Text("Service account name",
                        style: TextStyle(fontWeight: FontWeight.bold)),
                  ),
                ),
              ),
              Expanded(
                flex: 2,
                child: SelectableText("Environments",
                    style: TextStyle(fontWeight: FontWeight.bold)),
              ),
              Expanded(
                  flex: 3,
                  child: Text("Permissions",
                      style: TextStyle(fontWeight: FontWeight.bold))),
              Expanded(
                  flex: 4,
                  child: Text("Client & Server API Keys",
                      style: TextStyle(fontWeight: FontWeight.bold)))
            ],
          ),
        ),
        ListView.builder(
            shrinkWrap: true,
            itemCount: serviceAccountEnvs.serviceAccounts.length,
            itemBuilder: (context, index) {
              final serviceAccount = serviceAccountEnvs.serviceAccounts[index];

              if (!serviceAccount.permissions
                  .every((element) => element.permissions.isEmpty)) {
                return Card(
                  elevation: 4.0,
                  shadowColor: Colors.transparent,
                  child: Padding(
                    padding: const EdgeInsets.all(8.0),
                    child: Row(
                      children: [
                        Expanded(
                          flex: 2,
                          child: Align(
                            alignment: Alignment.centerLeft,
                            child: SelectionArea(
                              child: Text(serviceAccount.name,
                                  overflow: TextOverflow.ellipsis),
                            ),
                          ),
                        ),
                        Expanded(
                            flex: 6,
                            child: Column(
                              children: [
                                for (var env in serviceAccountEnvs.environments)
                                  if (serviceAccount.permissions
                                      .firstWhere(
                                          (p) => p.environmentId == env.id,
                                          orElse: () =>
                                              ServiceAccountPermission(
                                                  permissions: [],
                                                  environmentId: env.id))
                                      .permissions
                                      .isNotEmpty)
                                    Padding(
                                      padding: const EdgeInsets.all(8.0),
                                      child: Row(
                                        children: [
                                          Expanded(
                                            flex: 2,
                                            child: SelectableText(
                                              env.name,
                                            ),
                                          ),
                                          Expanded(
                                              flex: 3,
                                              child:
                                                  _ServiceAccountPermissionWidget(
                                                      env: env,
                                                      sa: serviceAccount)),
                                          Expanded(
                                              flex: 4,
                                              child: _ServiceAccountCopyWidget(
                                                  env: env,
                                                  sa: serviceAccount,
                                                  bloc: bloc))
                                        ],
                                      ),
                                    )
                              ],
                            )),
                      ],
                    ),
                  ),
                );
              } else {
                return Container();
              }
            }),
      ],
    );
  }
}

class _ServiceAccountPermissionWidget extends StatelessWidget {
  final Environment env;
  final ServiceAccount sa;

  const _ServiceAccountPermissionWidget(
      {Key? key, required this.env, required this.sa})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final account = sa.permissions.firstWhere((p) => p.environmentId == env.id,
        orElse: () => ServiceAccountPermission(
              environmentId: env.id,
              permissions: <RoleType>[],
            ));
    final perms = account.permissions;

    return Align(
      alignment: Alignment.centerLeft,
      child: SelectionArea(
        child: Container(
            child: perms.isNotEmpty
                ? Text(perms.map((p) => p.name).join(', '),
                    style: const TextStyle(
                        fontFamily: 'SourceCodePro',
                        fontSize: 12,
                        letterSpacing: 1.0))
                : Text('No permissions defined',
                    style: Theme.of(context).textTheme.bodySmall)),
      ),
    );
  }
}

class _ServiceAccountCopyWidget extends StatelessWidget {
  final ServiceAccount sa;
  final Environment env;
  final ServiceAccountEnvBloc bloc;

  const _ServiceAccountCopyWidget(
      {Key? key, required this.sa, required this.env, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final saPermission = sa.permissions.firstWhere(
        (p) => p.environmentId == env.id,
        orElse: () => ServiceAccountPermission(
            permissions: <RoleType>[], environmentId: env.id));
    var isScreenWide = MediaQuery.of(context).size.width >= 1350;

    return Align(
      alignment: Alignment.centerLeft,
      child: Flex(
          direction: isScreenWide ? Axis.horizontal : Axis.vertical,
          children: [
            if (saPermission.sdkUrlClientEval != null)
              FittedBox(
                child: Row(
                  children: [
                    Text(
                      'Client eval API Key ',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    FHCopyToClipboard(
                        copyString: saPermission.sdkUrlClientEval!,
                        tooltipMessage: saPermission.sdkUrlClientEval!),
                  ],
                ),
              ),
            if (saPermission.sdkUrlServerEval != null)
              const SizedBox(width: 16.0),
            if (saPermission.sdkUrlServerEval != null)
              FittedBox(
                child: Row(
                  children: [
                    Text(
                      'Server eval API Key',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    FHCopyToClipboard(
                        copyString: saPermission.sdkUrlServerEval!,
                        tooltipMessage: saPermission.sdkUrlServerEval!),
                  ],
                ),
              ),
            if (saPermission.sdkUrlClientEval == null)
              const Tooltip(
                message:
                    'API Key is unavailable because your current permissions for this environment are lower level',
                child: Icon(
                  Icons.error_outline,
                  size: 24.0,
                  color: Colors.red,
                ),
              ),
            if (saPermission.sdkUrlServerEval == null)
              const Tooltip(
                message:
                    'API Key is unavailable because your current permissions for this environment are lower level',
                child: Icon(
                  Icons.error_outline,
                  size: 24.0,
                  color: Colors.red,
                ),
              )
          ]),
    );
  }
}
