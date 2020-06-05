import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/widgets/common/fh_alert_dialog.dart';
import 'package:app_singleapp/widgets/common/fh_delete_thing.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_icon_button.dart';
import 'package:app_singleapp/widgets/user/list/list_users_bloc.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class PersonListWidget extends StatefulWidget {
  @override
  _PersonListWidgetState createState() => _PersonListWidgetState();
}

class _PersonListWidgetState extends State<PersonListWidget> {
  bool sortToggle = true;
  int sortColumnIndex = 0;

  @override
  Widget build(BuildContext context) {
    final BorderSide bs = BorderSide(color: Theme.of(context).dividerColor);
    ListUsersBloc bloc = BlocProvider.of(context);
    return StreamBuilder<List<Person>>(
        stream: bloc.personSearch,
        builder: (context, snapshot) {
          if (snapshot.hasError || snapshot.data == null) {
            return Container(
                padding: EdgeInsets.all(30), child: Text('Loading...'));
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
                  sortAscending: sortToggle,
                  sortColumnIndex: sortColumnIndex,
                  columns: [
                    DataColumn(
                        label: Text("Name"),
                        onSort: (columnIndex, ascending) {
                          onSortColumn(snapshot.data, columnIndex, ascending);
                        }),
                    DataColumn(
                      label: Text("Email"),
                      onSort: (columnIndex, ascending) {
                        onSortColumn(snapshot.data, columnIndex, ascending);
                      },
                    ),
                    DataColumn(
                      label: Text("Groups"),
                      onSort: (columnIndex, ascending) {
                        onSortColumn(snapshot.data, columnIndex, ascending);
                      },
                    ),
                    DataColumn(label: Text(""), onSort: (i, a) => {}),
                  ],
                  rows: [
                    for (Person p in snapshot.data)
                      DataRow(
                        cells: [
                          DataCell(p.name == null
                              ? Text("Not yet registered",
                                  style: Theme.of(context).textTheme.caption)
                              : Text(
                                  "${p.name}",
                                )),
                          DataCell(Text("${p.email}")),
                          DataCell(Text("${p.groups.length}")),
                          DataCell(Row(children: <Widget>[
                            FHIconButton(
                                icon: Icon(Icons.edit,
                                    color: Theme.of(context).buttonColor),
                                onPressed: () => {
                                      ManagementRepositoryClientBloc.router
                                          .navigateTo(context,
                                              "/manage-user",
                                          params: {
                                            'id': [p.id.id]
                                          },
                                      replace: true, transition: TransitionType.fadeIn)
                                    }),
                            FHIconButton(
                              icon: Icon(Icons.delete,
                                  color: Theme.of(context).buttonColor),
                              onPressed: () => bloc.mrClient
                                  .addOverlay((BuildContext context) {
                                return bloc.mrClient.person.id.id == p.id.id
                                    ? CantDeleteDialog(context, bloc)
                                    : DeleteDialogWidget(
                                        person: p,
                                        bloc: bloc,
                                      );
                              }),
                            )
                          ])),
                        ],
                      ),
                  ],
                ),
              ),
            ],
          );
        });
  }

  onSortColumn(List<Person> people, int columnIndex, bool ascending) {
    setState(() {
      if (columnIndex == 0) {
        if (ascending) {
          people.sort((a, b) {
            if (a.name != null && b.name != null) {
              return a.name.compareTo(b.name);
            }
            return ascending ? 1 : -1;
          });
        } else {
          people.sort((a, b) {
            if (a.name != null && b.name != null) {
              return b.name.compareTo(a.name);
            }
            return ascending ? -1 : 1;
          });
        }
      }
      if (columnIndex == 1) {
        if (ascending) {
          people.sort((a, b) => a.email.compareTo(b.email));
        } else {
          people.sort((a, b) => b.email.compareTo(a.email));
        }
      }
      if (columnIndex == 2) {
        if (ascending) {
          people.sort((a, b) => a.groups.length.compareTo(b.groups.length));
        } else {
          people.sort((a, b) => b.groups.length.compareTo(a.groups.length));
        }
      }
      if (this.sortColumnIndex == columnIndex) {
        this.sortToggle = !sortToggle;
      }
      this.sortColumnIndex = columnIndex;
    });
  }

  Widget CantDeleteDialog(BuildContext context, ListUsersBloc bloc) {
    return FHAlertDialog(
      title: Text("You can't delete yourself!"),
      content: Text(
          "To delete yourself from the system, you'll need to contact a site administrator."),
      actions: <Widget>[
        // usually buttons at the bottom of the dialog
        FHFlatButton(
          title: "Ok",
          onPressed: () {
            bloc.mrClient.removeOverlay();
          },
        )
      ],
    );
  }
}

class DeleteDialogWidget extends StatelessWidget {
  final Person person;
  final ListUsersBloc bloc;

  const DeleteDialogWidget(
      {Key key, @required this.person, @required this.bloc})
      : assert(person != null),
        assert(bloc != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHDeleteThingWarningWidget(
      thing: "user '${person.name == null ? person.email : person.name}",
      content: "This users will be removed from all groups and delete from the system. \n\nThis cannot be undone!",
      bloc: bloc.mrClient,
      deleteSelected: () async {
        try {
          await bloc.deletePerson(person.id.id, true);
          bloc.triggerSearch("");
          bloc.mrClient.addSnackbar(Text("User '${person.name}' deleted!"));
          return true;
        } catch (e, s) {
          bloc.mrClient.dialogError(e, s);
          return false;
        }
      },
    );
  }
}
