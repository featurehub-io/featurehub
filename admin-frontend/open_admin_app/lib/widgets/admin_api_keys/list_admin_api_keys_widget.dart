import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';
import 'package:open_admin_app/widgets/user/list/list_users_bloc.dart';

class AdminApiKeysListWidget extends StatefulWidget {
  const AdminApiKeysListWidget({Key? key}) : super(key: key);

  @override
  _AdminApiKeysListWidgetState createState() => _AdminApiKeysListWidgetState();
}

class _AdminApiKeysListWidgetState extends State<AdminApiKeysListWidget> {
  bool sortToggle = true;
  int sortColumnIndex = 0;

  @override
  Widget build(BuildContext context) {
    final bs = BorderSide(color: Theme.of(context).dividerColor);
    final bloc = BlocProvider.of<ListUsersBloc>(context);
    return StreamBuilder<List<Person>>(
        stream: bloc.adminAPIKeySearch,
        builder: (context, snapshot) {
          if (snapshot.hasError || snapshot.data == null) {
            return Container(
                padding: const EdgeInsets.all(30),
                child: const Text('Loading...'));
          }
          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: <Widget>[
              Container(
                decoration: BoxDecoration(
                  border: Border(
                    bottom: bs,
                    left: bs,
                    right: bs,
                  ),
                  color: Theme.of(context).cardColor,
                ),
                child: DataTable(
                  showCheckboxColumn: false,
                  sortAscending: sortToggle,
                  sortColumnIndex: sortColumnIndex,
                  columns: [
                    DataColumn(
                        label: const Text('Name'),
                        onSort: (columnIndex, ascending) {
                          onSortColumn(snapshot.data!, columnIndex, ascending);
                        }),
                    DataColumn(
                      label: const Text('Groups'),
                      onSort: (columnIndex, ascending) {
                        onSortColumn(snapshot.data!, columnIndex, ascending);
                      },
                    ),
                    DataColumn(label: const Padding(
                      padding: EdgeInsets.only(left:12.0),
                      child: Text('Actions'),
                    ), onSort: (i, a) => {}),
                  ],
                  rows: [
                    for (Person p in snapshot.data!)
                      DataRow(
                          cells: [
                            DataCell(Text(
                                    p.name!,
                                  )),
                            DataCell(Text('${p.groups.length}')),
                            DataCell(Row(children: <Widget>[
                              Tooltip(
                                message: "bla",
                                child: FHIconButton(
                                  icon: const Icon(Icons.info),
                                  onPressed: () => bloc.mrClient
                                      .addOverlay((BuildContext context) {
                                    return ListUserInfoDialog(bloc, p);
                                  }),
                                ),
                              ),
                              FHIconButton(
                                  icon: const Icon(Icons.edit),
                                  onPressed: () => {
                                        ManagementRepositoryClientBloc.router
                                            .navigateTo(context, '/edit-admin-api-key',
                                                params: {
                                              'id': [p.id!.id]
                                            })
                                      }),
                              // const SizedBox(
                              //   width: 8.0,
                              // ),
                              FHIconButton(
                                icon: const Icon(Icons.delete),
                                onPressed: () => bloc.mrClient
                                    .addOverlay((BuildContext context) {
                                  return bloc.mrClient.person.id!.id ==
                                          p.id!.id
                                      ? cantDeleteYourselfDialog(bloc)
                                      : DeleteDialogWidget(
                                          person: p,
                                          bloc: bloc,
                                        );
                                }),
                              ),
                            ])),
                          ],
                          onSelectChanged: (newValue) {
                            ManagementRepositoryClientBloc.router
                                .navigateTo(context, '/edit-admin-api-key', params: {
                              'id': [p.id!.id]
                            });
                          }),
                  ],
                ),
              ),
            ],
          );
        });
  }

  void onSortColumn(
      List<Person> people, int columnIndex, bool ascending) {
    setState(() {
      if (columnIndex == 0) {
        if (ascending) {
          people.sort((a, b) {
            if (a.name != null && b.name != null) {
              return a.name!.compareTo(b.name!);
            }
            return ascending ? 1 : -1;
          });
        } else {
          people.sort((a, b) {
            if (a.name != null && b.name != null) {
              return b.name!.compareTo(a.name!);
            }
            return ascending ? -1 : 1;
          });
        }
      }
      if (columnIndex == 1) {
        if (ascending) {
          people.sort((a, b) =>
              a.groups.length.compareTo(b.groups.length));
        } else {
          people.sort((a, b) =>
              b.groups.length.compareTo(a.groups.length));
        }
      }
      if (sortColumnIndex == columnIndex) {
        sortToggle = !sortToggle;
      }
      sortColumnIndex = columnIndex;
    });
  }

  Widget cantDeleteYourselfDialog(ListUsersBloc bloc) {
    return FHAlertDialog(
      title: const Text("You can't delete yourself!"),
      content: const Text(
          "To delete yourself from the system, you'll need to contact a site administrator."),
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

class ListUserInfoDialog extends StatelessWidget {
  final ListUsersBloc bloc;
  final Person entry;

  const ListUserInfoDialog(this.bloc, this.entry, {Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHAlertDialog(
      title: const Text(
        'Admin API Key details',
        style: TextStyle(fontSize: 22.0),
      ),
      content: _ListUserInfo(bloc: bloc, entry: entry),
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

class _ListUserInfo extends StatelessWidget {
  final ListUsersBloc bloc;
  final Person entry;

  const _ListUserInfo({Key? key, required this.bloc, required this.entry})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    entry.groups.sort((a, b) => a.name.compareTo(b.name));
    return SizedBox(
//      height: 400.0,
      width: 400.0,
      child: ListView(
        children: [
          _ListUserRow(
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
                                      style:
                                          Theme.of(context).textTheme.bodyText2,
                                    ))
                                .toList(),
                        ]))
              ],
            ),
        ],
      ),
    );
  }
}

class _ListUserRow extends StatelessWidget {
  final String title;
  final Widget child;

  const _ListUserRow({Key? key, required this.title, required this.child})
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

class DeleteDialogWidget extends StatelessWidget {
  final Person person;
  final ListUsersBloc bloc;

  const DeleteDialogWidget({Key? key, required this.person, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHDeleteThingWarningWidget(
      thing: "user '${person.name ?? person.email}'",
      content:
          'This user will be removed from all groups and deleted from the system. \n\nThis cannot be undone!',
      bloc: bloc.mrClient,
      deleteSelected: () async {
        try {
          await bloc.deletePerson(person.id!.id, true);
          bloc.triggerSearch('', false);
          bloc.mrClient.addSnackbar(Text("User '${person.name}' deleted!"));
          return true;
        } catch (e, s) {
          await bloc.mrClient.dialogError(e, s);
          return false;
        }
      },
    );
  }
}
