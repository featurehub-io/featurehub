import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/admin_sdk_service_account/admin_sa_reset_key_dialog_widget.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/user/list/list_users_bloc.dart';

class AdminServiceAccountsListWidget extends StatefulWidget {
  const AdminServiceAccountsListWidget({Key? key}) : super(key: key);

  @override
  _AdminServiceAccountsListWidgetState createState() =>
      _AdminServiceAccountsListWidgetState();
}

class _AdminServiceAccountsListWidgetState
    extends State<AdminServiceAccountsListWidget> {
  bool sortToggle = true;
  int sortColumnIndex = 0;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    BlocProvider.of<ListAdminServiceAccount>(context).triggerSearch(null);
  }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ListAdminServiceAccount>(context);
    return StreamBuilder<List<SearchPerson>>(
        stream: bloc.adminAPIKeySearch,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const FHLoadingIndicator();
          } else if (snapshot.connectionState == ConnectionState.active ||
              snapshot.connectionState == ConnectionState.done) {
            if (snapshot.hasError) {
              return const FHLoadingError();
            } else if (snapshot.hasData) {
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: <Widget>[
                  Card(
                    child: DataTable(
                      showCheckboxColumn: false,
                      sortAscending: sortToggle,
                      sortColumnIndex: sortColumnIndex,
                      columns: [
                        DataColumn(
                            label: const Text('Name'),
                            onSort: (columnIndex, ascending) {
                              onSortColumn(
                                  snapshot.data!, columnIndex, ascending);
                            }),
                        DataColumn(
                          label: const Text('Groups'),
                          onSort: (columnIndex, ascending) {
                            onSortColumn(
                                snapshot.data!, columnIndex, ascending);
                          },
                        ),
                        DataColumn(
                            label: const Padding(
                              padding: EdgeInsets.only(left: 12.0),
                              child: Text('Actions'),
                            ),
                            onSort: (i, a) => {}),
                      ],
                      rows: [
                        for (var p in snapshot.data!)
                          DataRow(
                              cells: [
                                DataCell(Text(
                                  p.name,
                                )),
                                DataCell(Text('${p.groupCount}')),
                                DataCell(Row(children: <Widget>[
                                  FHIconButton(
                                    icon: const Icon(Icons.info),
                                    onPressed: () => bloc.mrClient
                                        .addOverlay((BuildContext context) {
                                      return ServiceAccountInfoDialog(bloc, p);
                                    }),
                                  ),
                                  FHIconButton(
                                      icon: const Icon(Icons.edit),
                                      onPressed: () => {
                                            ManagementRepositoryClientBloc
                                                .router
                                                .navigateTo(context,
                                                    '/edit-admin-service-account',
                                                    params: {
                                                  'id': [p.id]
                                                })
                                          }),
                                  // const SizedBox(
                                  //   width: 8.0,
                                  // ),
                                  FHIconButton(
                                    icon: const Icon(
                                      Icons.refresh,
                                    ),
                                    tooltip: "Reset Admin SDK access token",
                                    onPressed: () => bloc.mrClient
                                        .addOverlay((BuildContext context) {
                                      return AdminSAKeyResetDialogWidget(
                                        person: p,
                                        bloc: bloc,
                                      );
                                    }),
                                  ),
                                  FHIconButton(
                                    icon: const Icon(Icons.delete),
                                    onPressed: () => bloc.mrClient
                                        .addOverlay((BuildContext context) {
                                      return DeleteAdminServiceAccountDialogWidget(
                                        person: p,
                                        bloc: bloc,
                                      );
                                    }),
                                  ),
                                ])),
                              ],
                              onSelectChanged: (newValue) {
                                ManagementRepositoryClientBloc.router
                                    .navigateTo(
                                        context, '/edit-admin-service-account',
                                        params: {
                                      'id': [p.id]
                                    });
                              }),
                      ],
                    ),
                  ),
                ],
              );
            }
          }
          return const SizedBox.shrink();
        });
  }

  void onSortColumn(
      List<SearchPerson> people, int columnIndex, bool ascending) {
    setState(() {
      if (columnIndex == 0) {
        if (ascending) {
          people.sort((a, b) {
            return a.name.compareTo(b.name);
          });
        } else {
          people.sort((a, b) {
            return b.name.compareTo(a.name);
          });
        }
      }
      if (columnIndex == 1) {
        if (ascending) {
          people.sort((a, b) => a.groupCount.compareTo(b.groupCount));
        } else {
          people.sort((a, b) => b.groupCount.compareTo(a.groupCount));
        }
      }
      if (sortColumnIndex == columnIndex) {
        sortToggle = !sortToggle;
      }
      sortColumnIndex = columnIndex;
    });
  }
}

class ServiceAccountInfoDialog extends StatelessWidget {
  final ListAdminServiceAccount bloc;
  final SearchPerson entry;

  const ServiceAccountInfoDialog(this.bloc, this.entry, {Key? key})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHAlertDialog(
      title: const Text(
        'Admin Service Account details',
        style: TextStyle(fontSize: 22.0),
      ),
      content: _AdminServiceAccountInfo(bloc: bloc, foundPerson: entry),
      actions: <Widget>[
        // usually buttons at the bottom of the dialog
        FHFlatButton(
          title: 'OK',
          onPressed: () {
            bloc.mrClient.removeOverlay();
          },
        )
      ],
    );
  }
}

class _AdminServiceAccountInfo extends StatelessWidget {
  final ListAdminServiceAccount bloc;
  final SearchPerson foundPerson;

  const _AdminServiceAccountInfo(
      {Key? key, required this.bloc, required this.foundPerson})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<Person>(
        future: bloc.getPerson(foundPerson.id),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
          return const FHLoadingIndicator();
          } else if (snapshot.connectionState == ConnectionState.active ||
          snapshot.connectionState == ConnectionState.done) {
          if (snapshot.hasError) {
          return const FHLoadingError();
          } else if (snapshot.hasData) {

          final entry = snapshot.data!;

          entry.groups.sort((a, b) => a.name.compareTo(b.name));

          return SizedBox(
//      height: 400.0,
            width: 400.0,
            child: ListView(
              children: [
                _AdminServiceAccountRow(
                  title: 'Name',
                  child: Text(entry.name!,
                      style: Theme.of(context).textTheme.bodyText1),
                ),
                const SizedBox(height: 16.0),
                const FHPageDivider(),
                const SizedBox(height: 16.0),
                if (entry.groups.isNotEmpty)
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
//              mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      Expanded(
                        flex: 1,
                        child: Text(
                          'Groups',
                          style: Theme.of(context).textTheme.caption,
                        ),
                      ),
                      Expanded(
                          flex: 5,
                          child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                if (entry.groups.isNotEmpty)
                                  ...entry.groups
                                      .map((e) => Text(
                                            e.name,
                                            style: Theme.of(context)
                                                .textTheme
                                                .bodyText2,
                                          ))
                                      .toList(),
                              ]))
                    ],
                  ),
              ],
            ),
          );
        }}
        return const SizedBox.shrink();
        });
  }
}

class _AdminServiceAccountRow extends StatelessWidget {
  final String title;
  final Widget child;

  const _AdminServiceAccountRow(
      {Key? key, required this.title, required this.child})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          flex: 1,
          child: Text(
            title,
            style: Theme.of(context).textTheme.caption,
          ),
        ),
        Expanded(flex: 5, child: child)
      ],
    );
  }
}

class DeleteAdminServiceAccountDialogWidget extends StatelessWidget {
  final SearchPerson person;
  final ListAdminServiceAccount bloc;

  const DeleteAdminServiceAccountDialogWidget(
      {Key? key, required this.person, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHDeleteThingWarningWidget(
      thing: "service account '${person.name}'",
      content:
          'This service account will be removed from all groups and deleted from the system. \n\nThis cannot be undone!',
      bloc: bloc.mrClient,
      deleteSelected: () async {
        try {
          await bloc.deletePerson(person.id, true);
          bloc.triggerSearch('');
          bloc.mrClient
              .addSnackbar(Text("Service account '${person.name}' deleted!"));
          return true;
        } catch (e, s) {
          await bloc.mrClient.dialogError(e, s);
          return false;
        }
      },
    );
  }
}
