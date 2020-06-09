import 'dart:ui';

import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/common/fh_footer_button_bar.dart';
import 'package:app_singleapp/widgets/common/fh_info_card.dart';
import 'package:app_singleapp/widgets/common/fh_link.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import 'manage_app_bloc.dart';

class ServiceAccountPermissionsWidget extends StatefulWidget {
  const ServiceAccountPermissionsWidget({Key key}) : super(key: key);

  @override
  _ServiceAccountPermissionState createState() =>
      _ServiceAccountPermissionState();
}

class _ServiceAccountPermissionState
    extends State<ServiceAccountPermissionsWidget> {
  String selectedServiceAccount;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    RouteChange route =
        BlocProvider.of<ManagementRepositoryClientBloc>(context).currentRoute;

    if (route.params['service-account'] != null) {
      selectedServiceAccount = route.params['service-account'][0];
      ManageAppBloc bloc = BlocProvider.of(context);
      bloc.selectServiceAccount(selectedServiceAccount);
    }
  }

  @override
  Widget build(BuildContext context) {
    ManageAppBloc bloc = BlocProvider.of(context);
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    return StreamBuilder<List<ServiceAccount>>(
        stream: bloc.serviceAccountsStream,
        builder: (context, snapshot) {
          if (!snapshot.hasData || snapshot.data.isEmpty) {
            return Row(
              mainAxisAlignment: MainAxisAlignment.start,
              children: <Widget>[
                Container(
                    padding: EdgeInsets.all(20),
                    child: Column(
                      children: <Widget>[
                        Text(
                            "There are no 'service accounts' in the '${bloc.portfolio.name}' portfolio."),
                        Container(
                          padding: EdgeInsets.only(top: 20, bottom: 20),
                          child: FHLinkWidget(
                              text:
                                  'Manage service accounts for this portfolio?',
                              href: '/manage-service-accounts'),
                        )
                      ],
                    )),
              ],
            );
          }

          // if there is only 1, force it to be selected
          if (selectedServiceAccount == null && snapshot.data.length == 1) {
            selectedServiceAccount = snapshot.data[0].id;
            bloc.selectServiceAccount(selectedServiceAccount);
          }

          return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Container(
                            padding: EdgeInsets.only(left: 10, top: 20),
                            child: Text(
                              "Service account",
                              style: Theme.of(context).textTheme.caption,
                            )),
                        Container(
                            padding: EdgeInsets.fromLTRB(10, 0, 0, 5),
                            child: serviceAccountDropdown(snapshot.data, bloc)),
                      ],
                    ),
                    Padding(
                      padding: const EdgeInsets.only(left: 16.0, top: 16.0),
                      child: FHInfoCardWidget(
                          message:
                              "The 'Lock/Unlock' and 'Change value' permissions \n"
                              "are so you can change these states through the API's \n"
                              "e.g., when running tests. \n \n"
                              "We strongly recommend setting production environments \n"
                              "with only 'Read' permission for service accounts."),
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
      child: DropdownButton(
        items: serviceAccounts.map((ServiceAccount serviceAccount) {
          return DropdownMenuItem<String>(
              value: serviceAccount.id,
              child: Text(
                serviceAccount.name,
              ));
        }).toList(),
        hint: Text(
          'Select service account',
          textAlign: TextAlign.end,
        ),
        onChanged: (value) {
          setState(() {
            selectedServiceAccount = value;
            bloc.selectServiceAccount(value);
          });
        },
        value: selectedServiceAccount,
      ),
    );
  }
}

class _ServiceAccountPermissionDetailWidget extends StatefulWidget {
  final ManagementRepositoryClientBloc mr;
  final ManageAppBloc bloc;

  const _ServiceAccountPermissionDetailWidget({
    Key key,
    @required this.mr,
    @required this.bloc,
  })  : assert(mr != null),
        assert(bloc != null),
        super(key: key);

  @override
  _ServiceAccountPermissionDetailState createState() =>
      _ServiceAccountPermissionDetailState();
}

class _ServiceAccountPermissionDetailState
    extends State<_ServiceAccountPermissionDetailWidget> {
  Map<String, ServiceAccountPermission> newServiceAccountPermission =
      Map<String, ServiceAccountPermission>();
  ServiceAccount currentServiceAccount;

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
                if (envSnapshot.data.isEmpty) {
                  return Row(
                    mainAxisAlignment: MainAxisAlignment.start,
                    children: <Widget>[
                      Container(
                          padding: EdgeInsets.all(20),
                          child: Text(
                              "You need to first create some 'Environments' for this application.")),
                    ],
                  );
                }

                if (currentServiceAccount == null ||
                    currentServiceAccount.id != saSnapshot.data.id) {
                  newServiceAccountPermission =
                      createMap(envSnapshot.data, saSnapshot.data);
                  currentServiceAccount = saSnapshot.data;
                }

                List<TableRow> rows = List();
                rows.add(getHeader());
                for (Environment env in envSnapshot.data) {
                  rows.add(TableRow(
                      decoration: BoxDecoration(
                          border: Border(
                              bottom: BorderSide(
                                  color: Theme.of(context).dividerColor))),
                      children: [
                        Container(
                            padding: EdgeInsets.fromLTRB(5, 15, 0, 0),
                            child: Text(env.name)),
                        getPermissionCheckbox(
                            env.id, ServiceAccountPermissionType.READ),
                        getPermissionCheckbox(
                            env.id, ServiceAccountPermissionType.TOGGLE_LOCK),
                        getPermissionCheckbox(env.id,
                            ServiceAccountPermissionType.TOGGLE_ENABLED),
                      ]));
                }

                Widget table = Table(children: rows);

                return Column(
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    Container(
                        padding: EdgeInsets.fromLTRB(5, 10, 0, 15),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.start,
                          children: <Widget>[
                            Text(
                                "Set the service account access to features for each environment",
                                style: Theme.of(context).textTheme.caption),
                          ],
                        )),
                    table,
                    FHButtonBar(children: [
                      FHFlatButtonTransparent(
                          onPressed: () {
                            this.currentServiceAccount = null;
                            widget.bloc
                                .selectServiceAccount(saSnapshot.data.id);
                            widget.bloc.mrClient.addSnackbar(Text(
                                "Service account '${saSnapshot.data.name}' reset!"));
                          },
                          title: 'Undo'),
                      FHFlatButton(
                          onPressed: () {
                            List<ServiceAccountPermission> newList = List();
                            this
                                .newServiceAccountPermission
                                .forEach((key, value) {
                              newList.add(value);
                            });
                            ServiceAccount newSa = saSnapshot.data;
                            newSa.permissions = newList;
                            widget.bloc
                                .updateServiceAccountPermissions(
                                    newSa.id, saSnapshot.data)
                                .then((serviceAccount) => widget.bloc.mrClient
                                    .addSnackbar(Text(
                                        "Service account '${serviceAccount?.name}' updated!")))
                                .catchError(widget.bloc.mrClient.dialogError);
                          },
                          title: 'Update'),
                    ]),
                  ],
                );
              });
        });
  }

  TableRow getHeader() {
    return TableRow(
        decoration: BoxDecoration(
            border: Border(
                bottom: BorderSide(color: Theme.of(context).dividerColor))),
        children: [
          Container(
            padding: EdgeInsets.fromLTRB(5, 0, 0, 15),
            child: Text(
              "Environment",
              style: Theme.of(context).textTheme.subtitle2,
            ),
          ),
          Center(
              child: Text(
            "Read",
            style: Theme.of(context).textTheme.subtitle2,
          )),
          Center(
              child: Text(
            "Lock/Unlock",
            style: Theme.of(context).textTheme.subtitle2,
          )),
          Center(
              child: Text(
            "Change value",
            style: Theme.of(context).textTheme.subtitle2,
          )),
        ]);
  }

  Checkbox getPermissionCheckbox(
      String envId, ServiceAccountPermissionType permissionType) {
    return Checkbox(
      value: this
          .newServiceAccountPermission[envId]
          .permissions
          .contains(permissionType),
      onChanged: (value) {
        setState(() {
          if (value) {
            this
                .newServiceAccountPermission[envId]
                .permissions
                .add(permissionType);
          } else {
            this
                .newServiceAccountPermission[envId]
                .permissions
                .remove(permissionType);
          }
        });
      },
    );
  }

  Map<String, ServiceAccountPermission> createMap(
      List<Environment> environments, ServiceAccount serviceAccount) {
    print("createMap serviceAccount: ${serviceAccount}");
    Map<String, ServiceAccountPermission> retMap =
        Map<String, ServiceAccountPermission>();
    environments.forEach((environment) {
      ServiceAccountPermission sap = serviceAccount.permissions.firstWhere(
          (item) => item.environmentId == environment.id,
          orElse: () => null);
      if (sap == null) {
        sap = ServiceAccountPermission();
        sap.environmentId = environment.id;
        sap.permissions = List<ServiceAccountPermissionType>();
      }
      print("sapt: ${sap.toString()}");
      retMap[environment.id] = sap;
    });
    return retMap;
  }
}
