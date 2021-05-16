import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/utils/utils.dart';
import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/copy_to_clipboard_html.dart';
import 'package:app_singleapp/widgets/common/decorations/fh_page_divider.dart';
import 'package:app_singleapp/widgets/common/fh_alert_dialog.dart';
import 'package:app_singleapp/widgets/common/fh_delete_thing.dart';
import 'package:app_singleapp/widgets/common/fh_icon_button.dart';
import 'package:app_singleapp/widgets/user/list/list_users_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
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
    final bs = BorderSide(color: Theme.of(context).dividerColor);
    final bloc = BlocProvider.of<ListUsersBloc>(context);
    return StreamBuilder<List<SearchPersonEntry>>(
        stream: bloc.personSearch,
        builder: (context, snapshot) {
          if (snapshot.hasError || snapshot.data == null) {
            return Container(
                padding: EdgeInsets.all(30), child: Text('Loading...'));
          }
          final allowedLocalIdentity = bloc.mrClient.identityProviders.hasLocal;
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
                        label: Text('Name'),
                        onSort: (columnIndex, ascending) {
                          onSortColumn(snapshot.data!, columnIndex, ascending);
                        }),
                    DataColumn(
                      label: Text('Email'),
                      onSort: (columnIndex, ascending) {
                        onSortColumn(snapshot.data!, columnIndex, ascending);
                      },
                    ),
                    DataColumn(
                      label: Text('Groups'),
                      onSort: (columnIndex, ascending) {
                        onSortColumn(snapshot.data!, columnIndex, ascending);
                      },
                    ),
                    DataColumn(label: Text(''), onSort: (i, a) => {}),
                  ],
                  rows: [
                    for (SearchPersonEntry p in snapshot.data!)
                      DataRow(
                        cells: [
                          DataCell(p.person.name == null
                              ? Text('Not yet registered',
                                  style: Theme.of(context).textTheme.caption)
                              : Text(
                                  '${p.person.name}',
                                )),
                          DataCell(Text('${p.person.email}')),
                          DataCell(Text('${p.person.groups.length}')),
                          DataCell(Row(children: <Widget>[
                            FHIconButton(
                              icon: Icon(Icons.info,
                                  color: _infoColour(p, allowedLocalIdentity)),
                              onPressed: () => bloc.mrClient
                                  .addOverlay((BuildContext context) {
                                return ListUserInfoDialog(bloc, p);
                              }),
                            ),
                            FHIconButton(
                                icon: Icon(Icons.edit),
                                onPressed: () => {
                                      ManagementRepositoryClientBloc.router
                                          .navigateTo(context, '/manage-user',
                                              params: {
                                            'id': [p.person.id!.id]
                                          })
                                    }),
                            SizedBox(
                              width: 20.0,
                            ),
                            FHIconButton(
                              icon: Icon(Icons.delete),
                              onPressed: () => bloc.mrClient
                                  .addOverlay((BuildContext context) {
                                return bloc.mrClient.person.id!.id ==
                                        p.person.id!.id
                                    ? CantDeleteDialog(bloc)
                                    : DeleteDialogWidget(
                                        person: p.person,
                                        bloc: bloc,
                                      );
                              }),
                            ),
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

  void onSortColumn(
      List<SearchPersonEntry> people, int columnIndex, bool ascending) {
    setState(() {
      if (columnIndex == 0) {
        if (ascending) {
          people.sort((a, b) {
            if (a.person.name != null && b.person.name != null) {
              return a.person.name!.compareTo(b.person.name!);
            }
            return ascending ? 1 : -1;
          });
        } else {
          people.sort((a, b) {
            if (a.person.name != null && b.person.name != null) {
              return b.person.name!.compareTo(a.person.name!);
            }
            return ascending ? -1 : 1;
          });
        }
      }
      if (columnIndex == 1) {
        if (ascending) {
          people.sort((a, b) => a.person.email!.compareTo(b.person.email!));
        } else {
          people.sort((a, b) => b.person.email!.compareTo(a.person.email!));
        }
      }
      if (columnIndex == 2) {
        if (ascending) {
          people.sort((a, b) =>
              a.person.groups.length.compareTo(b.person.groups.length));
        } else {
          people.sort((a, b) =>
              b.person.groups.length.compareTo(a.person.groups.length));
        }
      }
      if (sortColumnIndex == columnIndex) {
        sortToggle = !sortToggle;
      }
      sortColumnIndex = columnIndex;
    });
  }

  Widget CantDeleteDialog(ListUsersBloc bloc) {
    return FHAlertDialog(
      title: Text("You can't delete yourself!"),
      content: Text(
          "To delete yourself from the system, you'll need to contact a site administrator."),
      actions: <Widget>[
        // usually buttons at the bottom of the dialog
        FHFlatButton(
          title: 'Ok',
          onPressed: () {
            bloc.mrClient.removeOverlay();
          },
        )
      ],
    );
  }

  Color _infoColour(SearchPersonEntry entry, bool allowedLocalLogin) {
    if (!allowedLocalLogin) {
      return Theme.of(context).buttonColor;
    }

    if (entry.registration.expired) {
      return Colors.red;
    }

    return Colors.orange;
  }
}

class ListUserInfoDialog extends StatelessWidget {
  final ListUsersBloc bloc;
  final SearchPersonEntry entry;

  ListUserInfoDialog(this.bloc, this.entry);

  @override
  Widget build(BuildContext context) {
    return FHAlertDialog(
      title: Text(
        'User Information',
        style: TextStyle(fontSize: 22.0),
      ),
      content: _ListUserInfo(bloc: bloc, entry: entry),
      actions: <Widget>[
        // usually buttons at the bottom of the dialog
        FHFlatButton(
          title: 'Ok',
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
  final SearchPersonEntry entry;

  const _ListUserInfo({Key? key, required this.bloc, required this.entry})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final allowedLocalIdentity = bloc.mrClient.identityProviders.hasLocal;
    entry.person.groups.sort((a, b) => a.name.compareTo(b.name));
    return Container(
//      height: 400.0,
      width: 400.0,
      child: ListView(
        children: [
          _ListUserRow(
            title: 'Name',
            child: Text(entry.person.name!,
                style: Theme.of(context).textTheme.bodyText1),
          ),
          SizedBox(height: 8),
          _ListUserRow(
            title: 'Email',
            child: Text(entry.person.email!,
                style: Theme.of(context).textTheme.bodyText1),
          ),
          if (allowedLocalIdentity && !entry.registration.expired)
            Column(
              children: [
                SizedBox(height: 16),
                FHPageDivider(),
                SizedBox(height: 16),
                Align(
                  alignment: Alignment.centerLeft,
                  child: Text(
                    'Registration Url',
                    style: Theme.of(context).textTheme.caption,
                  ),
                ),
              ],
            ),
          if (allowedLocalIdentity && !entry.registration.expired)
            Row(
              children: [
                Expanded(
                    child: Text(
                        bloc.mrClient.registrationUrl(entry.registration.token),
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(fontSize: 11.0))),
                FHCopyToClipboard(
                  tooltipMessage: 'Copy URL to Clipboard',
                  copyString:
                      bloc.mrClient.registrationUrl(entry.registration.token),
                )
              ],
            ),
          if (allowedLocalIdentity && entry.registration.expired)
            Padding(
              padding: const EdgeInsets.only(top: 12.0, bottom: 4.0),
              child: Text(
                'Registration Expired',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
            ),
          if (allowedLocalIdentity && entry.registration.expired)
            FHCopyToClipboardFlatButton(
              caption: 'Renew registration and copy to clipboard',
              textProvider: () async {
                try {
                  final token = await bloc.mrClient.authServiceApi
                      .resetExpiredToken(entry.person.email!);
                  bloc.mrClient.addSnackbar(
                      Text('Registration renewed and copyied to clipboard'));
                  return bloc.mrClient.registrationUrl(token.registrationUrl);
                } catch (e, s) {
                  bloc.mrClient.addError(FHError.createError(e, s));
                }

                return null;
              },
            ),
          SizedBox(height: 16.0),
          FHPageDivider(),
          SizedBox(height: 16.0),
          if (entry.person.groups.isNotEmpty)
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
                          if (entry.person.groups.isNotEmpty)
                            ...entry.person.groups
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
      thing: "user '${person.name ?? person.email}",
      content:
          'This users will be removed from all groups and delete from the system. \n\nThis cannot be undone!',
      bloc: bloc.mrClient,
      deleteSelected: () async {
        try {
          await bloc.deletePerson(person.id!.id, true);
          bloc.triggerSearch('');
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
