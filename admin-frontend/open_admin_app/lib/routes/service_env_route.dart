import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/widgets/common/application_drop_down.dart';
import 'package:open_admin_app/widgets/common/copy_to_clipboard_html.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/service-accounts/service_accounts_env_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';

class ServiceAccountEnvRoute extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ServiceAccountEnvBloc>(context);
    return Container(
        padding: const EdgeInsets.fromLTRB(0, 8, 0, 0),
        child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Container(
                  padding: const EdgeInsets.fromLTRB(0, 8, 0, 0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Wrap(
                        children: [
                          Container(
                            padding: EdgeInsets.only(bottom: 10),
                            child: FHHeader(
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
                                if (snapshot.hasData &&
                                    snapshot.data!.isNotEmpty) {
                                  return Container(
                                      padding: EdgeInsets.only(bottom: 8),
                                      child: ApplicationDropDown(
                                          applications: snapshot.data!,
                                          bloc: bloc));
                                } else {
                                  return SizedBox.shrink();
                                }
                              }),
                          StreamBuilder<ReleasedPortfolio>(
                              stream: bloc.mrClient.personState
                                  .isCurrentPortfolioOrSuperAdmin,
                              builder: (context, snapshot) {
                                if (snapshot.data != null &&
                                    (snapshot.data!
                                            .currentPortfolioOrSuperAdmin ==
                                        true)) {
                                  return Padding(
                                    padding: const EdgeInsets.only(
                                        left: 16.0, bottom: 8.0),
                                    child: Container(
                                        child: FHFlatButtonTransparent(
                                            keepCase: true,
                                            title: 'Manage service accounts',
                                            onPressed: () => {
                                                  ManagementRepositoryClientBloc
                                                      .router
                                                      .navigateTo(
                                                    context,
                                                    '/manage-service-accounts',
                                                  )
                                                })),
                                  );
                                } else {
                                  return SizedBox.shrink();
                                }
                              }),
                        ],
                      ),
                      FHPageDivider(),
                      SizedBox(
                        height: 16.0,
                      ),
                      StreamBuilder<ServiceAccountEnvironments>(
                          stream: bloc.serviceAccountStream,
                          builder: (context, envSnapshot) {
                            if (!envSnapshot.hasData) {
                              return SizedBox.shrink();
                            }

                            if (envSnapshot.data!.serviceAccounts.isEmpty) {
                              return Text('No service accounts available',
                                  style: Theme.of(context).textTheme.caption);
                            }

                            return _ServiceAccountDisplayWidget(
                                serviceAccountEnvs: envSnapshot.data!);
                          }),
                    ],
                  )),
            ]));
  }
}

class _ServiceAccountDisplayWidget extends StatelessWidget {
  final ServiceAccountEnvironments serviceAccountEnvs;

  const _ServiceAccountDisplayWidget(
      {Key? key, required this.serviceAccountEnvs})
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
              color: Theme.of(context).cardColor,
              child: Padding(
                padding: const EdgeInsets.all(8.0),
                child: Row(
                  children: [
                    Expanded(
                      flex: 2,
                      child: Text(serviceAccount.name,
                          overflow: TextOverflow.ellipsis,
                          style: Theme.of(context)
                              .textTheme
                              .subtitle1!
                              .copyWith(
                                  color: Theme.of(context).brightness ==
                                          Brightness.light
                                      ? Theme.of(context).buttonColor
                                      : Theme.of(context).accentColor)),
                    ),
                    Expanded(
                        flex: 6,
                        child: Container(
                          child: Column(
                            children: [
                              for (var env in serviceAccountEnvs.environments)
                                if (serviceAccount.permissions
                                    .firstWhere(
                                        (p) => p.environmentId == env.id,
                                        orElse: () => ServiceAccountPermission(
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
                                              style: Theme.of(context)
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
                                                env: env, sa: serviceAccount))

//
                                      ],
                                    ),
                                  )
                            ],
                          ),
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
        orElse: () => ServiceAccountPermission(
              environmentId: env.id!,
              permissions: <RoleType>[],
            ));
    final perms = account.permissions;

    return Container(
        child: perms.isNotEmpty
            ? Text(perms.map((p) => p.name).join(', '),
                style: TextStyle(
                    fontFamily: 'Source', fontSize: 12, letterSpacing: 1.0))
            : Text('No permissions defined',
                style: Theme.of(context).textTheme.caption));
  }
}

class _ServiceAccountCopyWidget extends StatelessWidget {
  final ServiceAccount sa;
  final Environment env;

  const _ServiceAccountCopyWidget(
      {Key? key, required this.sa, required this.env})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final account = sa.permissions.firstWhere((p) => p.environmentId == env.id,
        orElse: () => ServiceAccountPermission(
            permissions: <RoleType>[], environmentId: env.id!));
    var isScreenWide = MediaQuery.of(context).size.width >= 1350;

    return Flex(
        direction: isScreenWide ? Axis.horizontal : Axis.vertical,
        children: [
          if (account.sdkUrlClientEval != null)
            FittedBox(
              child: Row(
                children: [
                  Text(
                    'Client eval API Key',
                    style: Theme.of(context).textTheme.caption,
                  ),
                  FHCopyToClipboard(
                      copyString: account.sdkUrlClientEval!,
                      tooltipMessage: account.sdkUrlClientEval!),
                ],
              ),
            ),
          if (account.sdkUrlServerEval != null)
            FittedBox(
              child: Row(
                children: [
                  Text(
                    'Server eval API Key',
                    style: Theme.of(context).textTheme.caption,
                  ),
                  FHCopyToClipboard(
                      copyString: account.sdkUrlServerEval!,
                      tooltipMessage: account.sdkUrlServerEval!),
                ],
              ),
            ),
          if (account.sdkUrlClientEval == null)
            Tooltip(
              message:
                  'SDK URL is unavailable because your current permissions for this environment are lower level',
              child: Icon(
                Feather.alert_circle,
                size: 24.0,
                color: Colors.red,
              ),
            ),
          if (account.sdkUrlServerEval == null)
            Tooltip(
              message:
                  'SDK URL is unavailable because your current permissions for this environment are lower level',
              child: Icon(
                Feather.alert_circle,
                size: 24.0,
                color: Colors.red,
              ),
            )
        ]);
  }
}
