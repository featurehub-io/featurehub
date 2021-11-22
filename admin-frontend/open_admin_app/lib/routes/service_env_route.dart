import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/widgets/common/application_drop_down.dart';
import 'package:open_admin_app/widgets/common/copy_to_clipboard_html.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/common/fh_underline_button.dart';
import 'package:open_admin_app/widgets/service-accounts/apikey_reset_dialog_widget.dart';
import 'package:open_admin_app/widgets/service-accounts/service_accounts_env_bloc.dart';

class ServiceAccountEnvRoute extends StatelessWidget {
  const ServiceAccountEnvRoute({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ServiceAccountEnvBloc>(context);
    return Container(
        padding: const EdgeInsets.fromLTRB(0, 8, 0, 0),
        child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Wrap(
                    children: [
                      Container(
                        padding: const EdgeInsets.only(bottom: 10),
                        child: const FHHeader(
                          title: 'API Keys',
                        ),
                      ),
                    ],
                  ),
                  Row(
                    children: [
                      StreamBuilder<List<Application>>(
                          stream: bloc.mrClient.streamValley
                              .currentPortfolioApplicationsStream,
                          builder: (context, snapshot) {
                            if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                              return Container(
                                  padding: const EdgeInsets.only(bottom: 8),
                                  child: ApplicationDropDown(
                                      applications: snapshot.data!,
                                      bloc: bloc));
                            } else {
                              return const SizedBox.shrink();
                            }
                          }),
                      StreamBuilder<ReleasedPortfolio?>(
                          stream: bloc.mrClient.personState
                              .isCurrentPortfolioOrSuperAdmin,
                          builder: (context, snapshot) {
                            if (snapshot.data != null &&
                                (snapshot.data!.currentPortfolioOrSuperAdmin ==
                                    true)) {
                              return Padding(
                                padding: const EdgeInsets.only(
                                    left: 8.0, bottom: 8.0),
                                child: FHUnderlineButton(
                                    keepCase: true,
                                    title: 'Go to service accounts settings',
                                    onPressed: () =>
                                    {
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
                  const FHPageDivider(),
                  const SizedBox(
                    height: 16.0,
                  ),
                  StreamBuilder<ServiceAccountEnvironments>(
                      stream: bloc.serviceAccountStream,
                      builder: (context, envSnapshot) {
                        if (!envSnapshot.hasData) {
                          return const SizedBox.shrink();
                        }

                        if (envSnapshot.data!.serviceAccounts.isEmpty) {
                          return Text('No service accounts available',
                              style: Theme
                                  .of(context)
                                  .textTheme
                                  .caption);
                        }

                        return _ServiceAccountDisplayWidget(
                            serviceAccountEnvs: envSnapshot.data!, bloc: bloc);
                      }),
                ],
              ),
            ]));
  }
}

class _ServiceAccountDisplayWidget extends StatelessWidget {
  final ServiceAccountEnvironments serviceAccountEnvs;
  final ServiceAccountEnvBloc bloc;

  const _ServiceAccountDisplayWidget(
      {Key? key, required this.serviceAccountEnvs, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    // filter out SA that don't have any permissions

    return ListView.builder(
        shrinkWrap: true,
        itemCount: serviceAccountEnvs.serviceAccounts.length,
        itemBuilder: (context, index) {
          final serviceAccount = serviceAccountEnvs.serviceAccounts[index];

          if (!serviceAccount.permissions
              .every((element) => element.permissions.isEmpty)) {
            return Card(
              color: Theme
                  .of(context)
                  .cardColor,
              child: Padding(
                padding: const EdgeInsets.all(8.0),
                child: Row(
                  children: [
                    Expanded(
                      flex: 2,
                      child: Text(serviceAccount.name,
                          overflow: TextOverflow.ellipsis,
                          style: Theme
                              .of(context)
                              .textTheme
                              .subtitle1!
                              .copyWith(
                              color: Theme
                                  .of(context)
                                  .brightness ==
                                  Brightness.light
                                  ? Theme
                                  .of(context)
                                  .buttonTheme
                                  .colorScheme
                                  ?.primary
                                  : Theme
                                  .of(context)
                                  .colorScheme
                                  .secondary)),
                    ),
                    Expanded(
                        flex: 6,
                        child: Column(
                          children: [
                            for (var env in serviceAccountEnvs.environments)
                              if (serviceAccount.permissions
                                  .firstWhere((p) => p.environmentId == env.id,
                                  orElse: () =>
                                      ServiceAccountPermission(
                                          permissions: [],
                                          environmentId: env.id!))
                                  .permissions
                                  .isNotEmpty)
                                Padding(
                                  padding: const EdgeInsets.all(8.0),
                                  child: Row(
                                    children: [
                                      Expanded(
                                        flex: 3,
                                        child: Text(env.name,
                                            style: Theme
                                                .of(context)
                                                .textTheme
                                                .bodyText2),
                                      ),
                                      Expanded(
                                          flex: 4,
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

//
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
        });
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
        orElse: () =>
            ServiceAccountPermission(
              environmentId: env.id!,
              permissions: <RoleType>[],
            ));
    final perms = account.permissions;

    return Container(
        child: perms.isNotEmpty
            ? Text(perms.map((p) => p.name).join(', '),
            style: const TextStyle(
                fontFamily: 'Source', fontSize: 12, letterSpacing: 1.0))
            : Text('No permissions defined',
            style: Theme
                .of(context)
                .textTheme
                .caption));
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
    final account = sa.permissions.firstWhere((p) => p.environmentId == env.id,
        orElse: () =>
            ServiceAccountPermission(
                permissions: <RoleType>[], environmentId: env.id!));
    var isScreenWide = MediaQuery
        .of(context)
        .size
        .width >= 1450;

    return Flex(
        direction: isScreenWide ? Axis.horizontal : Axis.vertical,
        children: [
          if (account.sdkUrlClientEval != null)
            FittedBox(
              child: Row(
                children: [
                  Text(
                    'Client eval API Key',
                    style: Theme
                        .of(context)
                        .textTheme
                        .caption,
                  ),
                  FHCopyToClipboard(
                      copyString: account.sdkUrlClientEval!,
                      tooltipMessage: account.sdkUrlClientEval!),
                  _ResetApiKeyWidget(bloc: bloc, saPermission: account, isClientKey: true)
                ],
              ),
            ),
          if (account.sdkUrlServerEval != null)
            FittedBox(
              child: Row(
                children: [
                  Text(
                    'Server eval API Key',
                    style: Theme
                        .of(context)
                        .textTheme
                        .caption,
                  ),
                  FHCopyToClipboard(
                      copyString: account.sdkUrlServerEval!,
                      tooltipMessage: account.sdkUrlServerEval!),
                  _ResetApiKeyWidget(bloc: bloc, saPermission: account, isClientKey: false)
                ],
              ),
            ),
          if (account.sdkUrlClientEval == null)
            const Tooltip(
              message:
              'API Key is unavailable because your current permissions for this environment are lower level',
              child: Icon(
                Feather.alert_circle,
                size: 24.0,
                color: Colors.red,
              ),
            ),
          if (account.sdkUrlServerEval == null)
            const Tooltip(
              message:
              'API Key is unavailable because your current permissions for this environment are lower level',
              child: Icon(
                Feather.alert_circle,
                size: 24.0,
                color: Colors.red,
              ),
            )
        ]);
  }
}

class _ResetApiKeyWidget extends StatelessWidget {
  final ServiceAccountPermission saPermission;
  final ServiceAccountEnvBloc bloc;

  final bool isClientKey;

  const _ResetApiKeyWidget(
      {Key? key, required this.saPermission, required this.bloc, required this.isClientKey})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return TextButton(
      onPressed: () =>
          bloc.mrClient.addOverlay((BuildContext context) {
            return ApiKeyResetDialogWidget(
              account: saPermission,
              bloc: bloc,
              isClientKey: isClientKey
            );
          }),
      child: const Text("Reset"),
      style: TextButton.styleFrom(primary: Colors.red),
    );
  }
}

