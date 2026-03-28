import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:logging/logging.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_widget.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_footer_button_bar.dart';
import 'package:open_admin_app/widgets/common/fh_info_card.dart';
import 'package:open_admin_app/widgets/common/fh_underline_button.dart';

import 'manage_app_bloc.dart';

final _log = Logger('ServiceAccountPermissionsWidget');

class ServiceAccountPermissionsWidget extends StatefulWidget {
  const ServiceAccountPermissionsWidget({Key? key}) : super(key: key);

  @override
  ServiceAccountPermissionState createState() =>
      ServiceAccountPermissionState();
}

class ServiceAccountPermissionState
    extends State<ServiceAccountPermissionsWidget> {
  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    final route =
        BlocProvider.of<ManagementRepositoryClientBloc>(context).currentRoute;

    if (route != null && route.params['service-account'] != null) {
      _log.fine(
          'Got route request for params ${route.params} so swapping service account');
      final bloc = BlocProvider.of<ManageAppBloc>(context);
      bloc.selectServiceAccount(route.params['service-account']![0]);
    }
  }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ManageAppBloc>(context);
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    return StreamBuilder<List<ServiceAccount>>(
        stream: bloc.serviceAccountsStream,
        builder: (context, snapshot) {
          if (!snapshot.hasData || snapshot.data!.isEmpty) {
            return Row(
              mainAxisAlignment: MainAxisAlignment.start,
              children: <Widget>[
                Container(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      children: <Widget>[
                        SelectableText(
                            AppLocalizations.of(context)!.noServiceAccountsInPortfolio(bloc.portfolio!.name)),
                        Container(
                          padding: const EdgeInsets.only(top: 20, bottom: 20),
                          child: FHUnderlineButton(
                            title: AppLocalizations.of(context)!.goToServiceAccountSettings,
                            onPressed: () => {
                              ManagementRepositoryClientBloc.router.navigateTo(
                                context,
                                '/service-accounts',
                              )
                            },
                          ),
                        )
                      ],
                    )),
              ],
            );
          }

          return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                const SizedBox(height: 16.0),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          AppLocalizations.of(context)!.serviceAccount,
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                        serviceAccountDropdown(snapshot.data!, bloc),
                      ],
                    ),
                    Padding(
                      padding: const EdgeInsets.only(left: 16.0),
                      child: FHInfoCardWidget(
                          message: AppLocalizations.of(context)!.serviceAccountInfoMessage),
                    ),
                    const SizedBox(
                      width: 32,
                    ),
                    FHExternalLinkWidget(
                      tooltipMessage: AppLocalizations.of(context)!.viewDocumentation,
                      link:
                          "https://docs.featurehub.io/featurehub/latest/service-accounts.html#_service_account_permissions",
                      icon: const Icon(Icons.arrow_outward_outlined),
                      label: AppLocalizations.of(context)!.serviceAccountsDocumentation,
                    ),
                  ],
                ),
                _ServiceAccountPermissionDetailWidget(bloc: bloc, mr: mrBloc)
              ]);
        });
  }

  Widget serviceAccountDropdown(
      List<ServiceAccount> serviceAccounts, ManageAppBloc bloc) {
    return Container(
      constraints: const BoxConstraints(maxWidth: 250),
      child: StreamBuilder<String?>(
          stream: bloc.currentServiceAccountIdStream,
          builder: (context, snapshot) {
            return InkWell(
              mouseCursor: SystemMouseCursors.click,
              child: DropdownButton(
                icon: const Padding(
                  padding: EdgeInsets.only(left: 8.0),
                  child: Icon(
                    Icons.keyboard_arrow_down,
                    size: 18,
                  ),
                ),
                isExpanded: true,
                isDense: true,
                items: serviceAccounts.map((ServiceAccount serviceAccount) {
                  return DropdownMenuItem<String>(
                      value: serviceAccount.id,
                      child: Text(
                        serviceAccount.name,
                        style: Theme.of(context).textTheme.bodyMedium,
                        overflow: TextOverflow.ellipsis,
                      ));
                }).toList(),
                hint: Text(
                  AppLocalizations.of(context)!.selectServiceAccount,
                  textAlign: TextAlign.end,
                ),
                onChanged: (String? value) {
                  if (value != null) {
                    setState(() {
                      bloc.selectServiceAccount(value);
                    });
                  }
                },
                value: snapshot.data,
              ),
            );
          }),
    );
  }
}

class _ServiceAccountPermissionDetailWidget extends StatefulWidget {
  final ManagementRepositoryClientBloc mr;
  final ManageAppBloc bloc;

  const _ServiceAccountPermissionDetailWidget({
    Key? key,
    required this.mr,
    required this.bloc,
  }) : super(key: key);

  @override
  _ServiceAccountPermissionDetailState createState() =>
      _ServiceAccountPermissionDetailState();
}

class _ServiceAccountPermissionDetailState
    extends State<_ServiceAccountPermissionDetailWidget> {
  Map<String, ServiceAccountPermission> newServiceAccountPermission = {};
  ServiceAccount? currentServiceAccount;

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<ServiceAccount>(
        stream: widget.bloc.serviceAccountStream,
        builder: (context, saSnapshot) {
          if (!saSnapshot.hasData) {
            return Container();
          }

          return StreamBuilder<List<Environment>>(
              stream: widget.bloc.environmentsStream,
              builder: (context, envSnapshot) {
                if (!envSnapshot.hasData) {
                  return Container();
                }
                if (envSnapshot.data!.isEmpty) {
                  return Row(
                    mainAxisAlignment: MainAxisAlignment.start,
                    children: <Widget>[
                      Container(
                          padding: const EdgeInsets.all(20),
                          child: SelectableText(
                              AppLocalizations.of(context)!.needToCreateEnvironmentsFirst)),
                    ],
                  );
                }

                newServiceAccountPermission =
                    createMap(envSnapshot.data!, saSnapshot.data!);
                currentServiceAccount = saSnapshot.data;

                final rows = <TableRow>[];
                rows.add(getHeader());
                for (var env in envSnapshot.data!) {
                  rows.add(TableRow(children: [
                    Padding(
                      padding: const EdgeInsets.all(8.0),
                      child: SelectableText(env.name),
                    ),
                    PermissionsCheckbox(
                        newServiceAccountPermission:
                            newServiceAccountPermission,
                        envId: env.id,
                        permissionType: RoleType.READ),
                    PermissionsCheckbox(
                        newServiceAccountPermission:
                            newServiceAccountPermission,
                        envId: env.id,
                        permissionType: RoleType.LOCK),
                    PermissionsCheckbox(
                        newServiceAccountPermission:
                            newServiceAccountPermission,
                        envId: env.id,
                        permissionType: RoleType.UNLOCK),
                    PermissionsCheckbox(
                        newServiceAccountPermission:
                            newServiceAccountPermission,
                        envId: env.id,
                        permissionType: RoleType.CHANGE_VALUE),
                    if (widget.bloc.mrClient.identityProviders
                        .featurePropertyExtendedDataEnabled)
                      PermissionsCheckbox(
                          newServiceAccountPermission:
                              newServiceAccountPermission,
                          envId: env.id,
                          permissionType: RoleType.EXTENDED_DATA),
                  ]));
                }

                return Column(
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    Container(
                        padding: const EdgeInsets.fromLTRB(0, 24, 0, 16),
                        child: Center(
                          child: SelectableText(
                              AppLocalizations.of(context)!.setServiceAccountPermissions,
                              style: Theme.of(context).textTheme.bodySmall),
                        )),
                    Card(
                      child: Table(
                          defaultVerticalAlignment:
                              TableCellVerticalAlignment.middle,
                          border: TableBorder(
                              horizontalInside: BorderSide(
                                  color: Theme.of(context)
                                      .dividerColor
                                      .withAlpha(128))),
                          children: rows),
                    ),
                    FHButtonBar(children: [
                      FHFlatButtonTransparent(
                        onPressed: () {
                          currentServiceAccount = null;
                          widget.bloc.selectServiceAccount(saSnapshot.data!.id);
                        },
                        title: AppLocalizations.of(context)!.cancel,
                        keepCase: true,
                      ),
                      FHFlatButton(
                          onPressed: () {
                            final newList = <ServiceAccountPermission>[];
                            newServiceAccountPermission.forEach((key, value) {
                              newList.add(value);
                            });
                            final newSa = saSnapshot.data!;
                            newSa.permissions = newList;
                            widget.bloc
                                .updateServiceAccountPermissions(
                                    newSa.id,
                                    saSnapshot.data!,
                                    (envSnapshot.data?.isNotEmpty == true)
                                        ? envSnapshot.data?.first.applicationId
                                        : null)
                                .then((serviceAccount) => widget.bloc.mrClient
                                    .addSnackbar(Text(
                                        AppLocalizations.of(context)!.serviceAccountUpdated(serviceAccount?.name ?? '<unknown>'))))
                                .catchError((e, s) {
                              widget.bloc.mrClient.dialogError(e, s);
                            });
                          },
                          title: AppLocalizations.of(context)!.update),
                    ]),
                  ],
                );
              });
        });
  }

  TableRow getHeader() {
    final l10n = AppLocalizations.of(context)!;
    var headerStyle = Theme.of(context)
        .textTheme
        .titleSmall!
        .copyWith(fontWeight: FontWeight.bold);
    return TableRow(children: [
      const Text(''),
      Center(child: Padding(padding: const EdgeInsets.all(12.0), child: Text(l10n.permRead, style: headerStyle))),
      Center(child: Padding(padding: const EdgeInsets.all(12.0), child: Text(l10n.permLock, style: headerStyle))),
      Center(child: Padding(padding: const EdgeInsets.all(12.0), child: Text(l10n.permUnlock, style: headerStyle))),
      Center(child: Padding(padding: const EdgeInsets.all(12.0), child: Text(l10n.permChangeValue, style: headerStyle))),
      if (widget.bloc.mrClient.identityProviders.featurePropertyExtendedDataEnabled)
        Center(child: Padding(padding: const EdgeInsets.all(12.0), child: Text(l10n.permReadExtendedData, style: headerStyle))),
    ]);
  }

  Map<String, ServiceAccountPermission> createMap(
      List<Environment> environments, ServiceAccount serviceAccount) {
    final retMap = <String, ServiceAccountPermission>{};

    for (var environment in environments) {
      final sap = serviceAccount.permissions
          .firstWhere((item) => item.environmentId == environment.id,
              orElse: () => ServiceAccountPermission(
                    environmentId: environment.id,
                    permissions: <RoleType>[],
                  ));

      retMap[environment.id] = sap;
    }

    return retMap;
  }
}

class PermissionsCheckbox extends StatefulWidget {
  final Map<String, ServiceAccountPermission> newServiceAccountPermission;
  final String envId;
  final RoleType permissionType;
  const PermissionsCheckbox(
      {Key? key,
      required this.newServiceAccountPermission,
      required this.envId,
      required this.permissionType})
      : super(key: key);

  @override
  State<PermissionsCheckbox> createState() => _PermissionsCheckboxState();
}

class _PermissionsCheckboxState extends State<PermissionsCheckbox> {
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: Checkbox(
        value: widget.newServiceAccountPermission[widget.envId]!.permissions
            .contains(widget.permissionType),
        onChanged: (bool? value) {
          setState(() {
            if (value == true) {
              widget.newServiceAccountPermission[widget.envId]!.permissions
                  .add(widget.permissionType);
            } else {
              widget.newServiceAccountPermission[widget.envId]!.permissions
                  .remove(widget.permissionType);
            }
          });
        },
      ),
    );
  }
}
