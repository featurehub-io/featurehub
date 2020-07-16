import 'dart:html' as html;

import 'package:app_singleapp/utils/custom_cursor.dart';
import 'package:app_singleapp/widgets/common/application_drop_down.dart';
import 'package:app_singleapp/widgets/common/decorations/fh_page_divider.dart';
import 'package:app_singleapp/widgets/common/fh_header.dart';
import 'package:app_singleapp/widgets/service-accounts/service_accounts_env_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
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
                    return SizedBox.shrink();
                  }
                }),
            Container(
              padding: EdgeInsets.only(bottom: 10),
              child: FHHeader(
                title: 'Service Accounts',
                children: <Widget>[],
              ),
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

                  if (envSnapshot.data.serviceAccounts.isEmpty) {
                    return Text('No service accounts available',
                        style: Theme.of(context).textTheme.caption);
                  }

                  return _ServiceAccountDisplayWidget(
                      serviceAccounts: envSnapshot.data);
                }),
          ],
        ));
  }
}

class _ServiceAccountDisplayWidget extends StatelessWidget {
  final ServiceAccountEnvironments serviceAccounts;

  const _ServiceAccountDisplayWidget({Key key, this.serviceAccounts})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ListView.separated(
        separatorBuilder: (BuildContext context, int index) =>
            Divider(height: 1.0),
        shrinkWrap: true,
        itemCount: serviceAccounts.environments.length,
        itemBuilder: (context, index) {
          final env = serviceAccounts.environments[index];
          return Card(
            color: Colors.white,
            child: Padding(
              padding: const EdgeInsets.all(8.0),
              child: Row(
                children: [
                  Expanded(
                    flex: 1,
                    child: Container(
//                    color: Colors.blue,
                        child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(env.name,
                            style: Theme.of(context)
                                .textTheme
                                .subtitle1
                                .copyWith(
                                    color: Theme.of(context).primaryColor)),
                      ],
                    )),
                  ),
                  Expanded(
                      flex: 5,
                      child: Container(
//                      color: Colors.red,
                        child: Column(
                          children: [
                            for (var sa in serviceAccounts.serviceAccounts)
                              Padding(
                                padding: const EdgeInsets.all(8.0),
                                child: Row(
                                  children: [
                                    Expanded(
                                      flex: 1,
                                      child: Text(sa.name,
                                          style: Theme.of(context)
                                              .textTheme
                                              .bodyText2),
                                    ),
                                    Expanded(
                                        flex: 4,
                                        child: _ServiceAccountPermissionWidget(
                                            env: env, sa: sa)),
//
                                  ],
                                ),
                              )
                          ],
                        ),
                      ))
                ],
              ),
            ),
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
    final account = sa.permissions.firstWhere((p) => p.environmentId == env.id,
        orElse: () => ServiceAccountPermission()..permissions = <RoleType>[]);
    final perms = account.permissions;

    return Container(
        child: perms.isNotEmpty
            ? Row(
                mainAxisAlignment: MainAxisAlignment.start,
                children: [
                  Text(
                      perms
                          .map((p) =>
                              RoleTypeTypeTransformer.toJson(p).toString())
                          .join(', '),
                      style: TextStyle(
                          fontFamily: 'Source',
                          fontSize: 12,
                          letterSpacing: 1.0)),
                  SizedBox(
                    width: 16.0,
                  ),
                  if (account.sdkUrl != null)
                    _CopyServiceAccountUrlToClipboard(account: account),
                  if (account.sdkUrl == null)
                    Tooltip(
                      message:
                          'SDK URL is unavailable because your current permissions for this environment are lower level',
                      child: Icon(
                        Feather.alert_circle,
                        size: 24.0,
                        color: Colors.red,
                      ),
                    )
                ],
              )
            : Row(
                children: [
                  Text('No permissions defined',
                      style: Theme.of(context).textTheme.caption),
                ],
              ));
  }
}

class _CopyServiceAccountUrlToClipboard extends StatelessWidget {
  final ServiceAccountPermission account;

  const _CopyServiceAccountUrlToClipboard({Key key, @required this.account})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return
//      sa.sdkUrl  != null ?
        Tooltip(
      message: 'Copy SDK Url to clipboard',
      child: CustomCursor(
        child: InkWell(
          mouseCursor: SystemMouseCursors.click,
          borderRadius: BorderRadius.circular(10.0),
          hoverColor: Theme.of(context).primaryColorLight,
          splashColor: Theme.of(context).primaryColor,
          child: Container(
              width: 20,
              height: 20,
              child: Icon(Icons.content_copy, size: 16.0)),
          onTap: () async {
            await html.window.navigator.permissions
                .query({'name': 'clipboard-write'});
            await html.window.navigator.clipboard.writeText(account.sdkUrl);
          },
        ),
      ),
    )
//        : SizedBox.shrink()
        ;
  }
}
