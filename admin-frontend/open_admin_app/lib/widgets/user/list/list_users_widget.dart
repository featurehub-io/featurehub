import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/copy_to_clipboard_html.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/user/list/list_users_bloc.dart';

class PersonListWidget extends StatefulWidget {
  const PersonListWidget({Key? key}) : super(key: key);

  @override
  _PersonListWidgetState createState() => _PersonListWidgetState();
}

class _PersonListWidgetState extends State<PersonListWidget> {
  bool sortToggle = true;
  int sortColumnIndex = 0;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    BlocProvider.of<ListPersonBloc>(context).triggerSearch(null);
  }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ListPersonBloc>(context);
    return StreamBuilder<List<SearchPersonEntry>>(
        stream: bloc.personSearch,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const FHLoadingIndicator();
          } else if (snapshot.connectionState == ConnectionState.active ||
              snapshot.connectionState == ConnectionState.done) {
            if (snapshot.hasError) {
              return const FHLoadingError();
            } else if (snapshot.hasData) {
              final allowedLocalIdentity =
                  bloc.mrClient.identityProviders.hasLocal;
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
                          label: const Text('Email'),
                          onSort: (columnIndex, ascending) {
                            onSortColumn(
                                snapshot.data!, columnIndex, ascending);
                          },
                        ),
                        DataColumn(
                          label: const Text('Groups'),
                          onSort: (columnIndex, ascending) {
                            onSortColumn(
                                snapshot.data!, columnIndex, ascending);
                          },
                        ),
                        DataColumn(
                          label: const Text('Last logged in'),
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
                        for (SearchPersonEntry p in snapshot.data!)
                          DataRow(
                              cells: [
                                DataCell(p.person.name == "No name"
                                    ? Text('Not yet registered',
                                        style:
                                            Theme.of(context).textTheme.caption)
                                    : Text(
                                        p.person.name,
                                      )),
                                DataCell(Text(p.person.email)),
                                DataCell(Text('${p.person.groupCount}')),
                                DataCell(Text(
                                    '${p.person.whenLastAuthenticated?.toLocal() ?? ""}')),
                                DataCell(Row(children: <Widget>[
                                  Tooltip(
                                    message:
                                    _infoTooltip(p, allowedLocalIdentity),
                                    child: FHIconButton(
                                      icon: Icon(Icons.info, color: _infoColour(p, allowedLocalIdentity)),
                                      onPressed: () => bloc.mrClient
                                          .addOverlay((BuildContext context) {
                                        return ListUserInfoDialog(bloc, p);
                                      }),
                                    ),
                                  ),
                                  FHIconButton(
                                      icon: const Icon(Icons.edit),
                                      onPressed: () => {
                                            ManagementRepositoryClientBloc
                                                .router
                                                .navigateTo(
                                                    context, '/manage-user',
                                                    params: {
                                                  'id': [p.person.id]
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
                                              p.person.id
                                          ? cantDeleteYourselfDialog(bloc)
                                          : DeleteDialogWidget(
                                              person: p.person,
                                              bloc: bloc,
                                            );
                                    }),
                                  ),
                                ])),
                              ],
                              onSelectChanged: (newValue) {
                                ManagementRepositoryClientBloc.router
                                    .navigateTo(context, '/manage-user',
                                        params: {
                                      'id': [p.person.id]
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
      List<SearchPersonEntry> people, int columnIndex, bool ascending) {
    setState(() {
      if (columnIndex == 0) {
        if (ascending) {
          people.sort((a, b) {
            return a.person.name
                .toLowerCase()
                .compareTo(b.person.name.toLowerCase());
          });
        } else {
          people.sort((a, b) {
            return b.person.name
                .toLowerCase()
                .compareTo(a.person.name.toLowerCase());
          });
        }
      }
      if (columnIndex == 1) {
        if (ascending) {
          people.sort((a, b) => a.person.email
              .toLowerCase()
              .compareTo(b.person.email.toLowerCase()));
        } else {
          people.sort((a, b) => b.person.email
              .toLowerCase()
              .compareTo(a.person.email.toLowerCase()));
        }
      }
      if (columnIndex == 2) {
        if (ascending) {
          people.sort(
              (a, b) => a.person.groupCount.compareTo(b.person.groupCount));
        } else {
          people.sort(
              (a, b) => b.person.groupCount.compareTo(a.person.groupCount));
        }
      }
      if (columnIndex == 3) {
        if (ascending) {
          people.sort((a, b) {
            if (a.person.whenLastAuthenticated != null &&
                b.person.whenLastAuthenticated != null) {
              return a.person.whenLastAuthenticated!
                  .compareTo(b.person.whenLastAuthenticated!);
            }
            return ascending ? 1 : -1;
          });
        } else {
          people.sort((a, b) {
            if (a.person.whenLastAuthenticated != null &&
                b.person.whenLastAuthenticated != null) {
              return b.person.whenLastAuthenticated!
                  .compareTo(a.person.whenLastAuthenticated!);
            }
            return ascending ? -1 : 1;
          });
        }
      }
      if (sortColumnIndex == columnIndex) {
        sortToggle = !sortToggle;
      }
      sortColumnIndex = columnIndex;
    });
  }

  Widget cantDeleteYourselfDialog(ListPersonBloc bloc) {
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
  final ListPersonBloc bloc;
  final SearchPersonEntry entry;

  const ListUserInfoDialog(this.bloc, this.entry, {Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHAlertDialog(
      title: const Text(
        'User information',
        style: TextStyle(fontSize: 22.0),
      ),
      content: _ListUserInfo(bloc: bloc, foundPerson: entry),
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
  final ListPersonBloc bloc;
  final SearchPersonEntry foundPerson;

  const _ListUserInfo({Key? key, required this.bloc, required this.foundPerson})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final allowedLocalIdentity = bloc.mrClient.identityProviders.hasLocal;

    return FutureBuilder<Person>(
        future: bloc.getPerson(foundPerson.person.id),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const FHLoadingIndicator();
          } else if (snapshot.connectionState == ConnectionState.active ||
              snapshot.connectionState == ConnectionState.done) {
            if (snapshot.hasError) {
              return const FHLoadingError();
            } else if (snapshot.hasData) {
              final person = snapshot.data!;

              person.groups.sort((a, b) => a.name.compareTo(b.name));

              return SizedBox(
                height: 250.0,
                width: 400.0,
                child: ListView(
                  children: [
                    _ListUserRow(
                      title: 'Name',
                      child: Text(foundPerson.person.name,
                          style: Theme.of(context).textTheme.bodyText1),
                    ),
                    const SizedBox(height: 8),
                    _ListUserRow(
                      title: 'Email',
                      child: Text(foundPerson.person.email,
                          style: Theme.of(context).textTheme.bodyText1),
                    ),
                    if (allowedLocalIdentity &&
                        !foundPerson.registration.expired &&
                        foundPerson.registration.token.isNotEmpty)
                      Column(
                        children: [
                          const SizedBox(height: 16),
                          const FHPageDivider(),
                          const SizedBox(height: 16),
                          Align(
                            alignment: Alignment.centerLeft,
                            child: Text(
                              'Registration URL',
                              style: Theme.of(context).textTheme.caption,
                            ),
                          ),
                        ],
                      ),
                    if (allowedLocalIdentity &&
                        !foundPerson.registration.expired &&
                        foundPerson.registration.token.isNotEmpty)
                      Row(
                        children: [
                          Expanded(
                              child: Text(
                                  bloc.mrClient.registrationUrl(
                                      foundPerson.registration.token),
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(fontSize: 11.0))),
                          FHCopyToClipboard(
                            tooltipMessage: 'Copy URL to Clipboard',
                            copyString: bloc.mrClient.registrationUrl(
                                foundPerson.registration.token),
                          )
                        ],
                      ),
                    if (allowedLocalIdentity &&
                        foundPerson.registration.expired)
                      const Padding(
                        padding: EdgeInsets.only(top: 12.0, bottom: 4.0),
                        child: Text(
                          'Registration expired',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                      ),
                    if (allowedLocalIdentity &&
                        foundPerson.registration.expired)
                      FHCopyToClipboardFlatButton(
                        caption: 'Renew registration URL and copy to clipboard',
                        textProvider: () async {
                          try {
                            final token = await bloc.mrClient.authServiceApi
                                .resetExpiredToken(foundPerson.person.email);
                            bloc.mrClient.addSnackbar(const Text(
                                'Registration URL renewed and copied to clipboard'));
                            return bloc.mrClient.registrationUrl(token.token);
                          } catch (e, s) {
                            bloc.mrClient.addError(FHError.createError(e, s));
                          }

                          return null;
                        },
                      ),
                    const SizedBox(height: 16.0),
                    const FHPageDivider(),
                    const SizedBox(height: 16.0),
                    if (person.groups.isNotEmpty)
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
                                    if (person.groups.isNotEmpty)
                                      ...person.groups
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
            }
          }
          return const SizedBox.shrink();
        });
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
  final SearchPerson person;
  final ListPersonBloc bloc;

  const DeleteDialogWidget({Key? key, required this.person, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHDeleteThingWarningWidget(
      thing: "user '${person.name}'",
      content:
          'This user will be removed from all groups and deleted from the system. \n\nThis cannot be undone!',
      bloc: bloc.mrClient,
      deleteSelected: () async {
        try {
          await bloc.deletePerson(person.id, true);
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

String _infoTooltip(SearchPersonEntry entry, bool allowedLocalLogin) {
  if (entry.registration.expired) {
    return "Registration expired";
  }
  return "";
}

Color _infoColour(SearchPersonEntry entry, bool allowedLocalLogin) {

  if (entry.registration.expired) {
    return Colors.red;
  }

  return Colors.green;
}
