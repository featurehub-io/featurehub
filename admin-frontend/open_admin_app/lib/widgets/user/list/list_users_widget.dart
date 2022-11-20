import 'dart:async';

import 'package:advanced_datatable/advanced_datatable_source.dart';
import 'package:advanced_datatable/datatable.dart';
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
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
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
  var sortIndex = 0;
  var sortAsc = true;
  var rowsPerPage = AdvancedPaginatedDataTable.defaultRowsPerPage;

  late PersonDataTableSource source;
  late ListUsersBloc bloc;

  @override
  void initState() {
    super.initState();
    bloc = BlocProvider.of<ListUsersBloc>(context);
    source = PersonDataTableSource(bloc, context);
  }

  @override
  Widget build(BuildContext context) {
    final _debouncer = Debouncer(milliseconds: 500);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Container(
          constraints: const BoxConstraints(maxWidth: 300),
          child: TextField(
            decoration: const InputDecoration(
              hintText: 'Search users',
              icon: Icon(Icons.search),
            ),
            onChanged: (val) {
              _debouncer.run(() {
                source.filterServerSide(val);
              });
            },
          ),
        ),
        const SizedBox(height: 16.0),
        SelectionArea(
          child: AdvancedPaginatedDataTable(
            rowsPerPage: rowsPerPage,
            showCheckboxColumn: false,
            showFirstLastButtons: true,
            addEmptyRows: false,
            availableRowsPerPage: const [10, 20, 50, 100],
            sortAscending: sortAsc,
            sortColumnIndex: sortIndex,
            onRowsPerPageChanged: (newRowsPerPage) {
              if (newRowsPerPage != null) {
                setState(() {
                  rowsPerPage = newRowsPerPage;
                });
              }
            },
            columns: [
              DataColumn(label: const Text('Name'), onSort: setSort),
              const DataColumn(
                label: Text('Email'),
              ),
              const DataColumn(
                label: Text('Groups'),
              ),
              const DataColumn(
                label: Text('Last logged in'),
              ),
              const DataColumn(
                label: Padding(
                  padding: EdgeInsets.only(left: 12.0),
                  child: Text('Actions'),
                ),
              ),
            ],
            source: source,
          ),
        ),
      ],
    );
  }

  void setSort(int i, bool asc) => setState(() {
        sortIndex = i;
        sortAsc = asc;
      });
}

// The "source" of the table
class PersonDataTableSource extends AdvancedDataTableSource<SearchPersonEntry> {
  String lastSearchTerm = '';
  final ListUsersBloc bloc;
  final BuildContext context;

  PersonDataTableSource(this.bloc, this.context);

  @override
  bool get isRowCountApproximate => false;

  @override
  int get selectedRowCount => 0;

  void filterServerSide(String filterQuery) {
    lastSearchTerm = filterQuery.toLowerCase().trim();
    setNextView();
  }

  @override
  Future<RemoteDataSourceDetails<SearchPersonEntry>> getNextPage(
      NextPageRequest pageRequest) async {
    final data = await bloc.findPeople(
        pageRequest.pageSize,
        pageRequest.offset,
        lastSearchTerm.isNotEmpty ? lastSearchTerm : null,
        (pageRequest.sortAscending ?? true) == true
            ? SortOrder.ASC
            : SortOrder.DESC,
        PersonType.person);
    final userList = bloc.transformPeople(data);
    return RemoteDataSourceDetails(
      data.max,
      userList.toList(),
      filteredRows: null, //the total amount of filtered rows, null by default
    );
  }

  @override
  DataRow getRow(int index) {
    final _personEntry = lastDetails!.rows[index];
    final allowedLocalIdentity = bloc.mrClient.identityProviders.hasLocal;
    return DataRow.byIndex(
        index: index,
        cells: [
          DataCell(_personEntry.person.name == "No name"
              ? Text('Not yet registered',
                  style: Theme.of(context).textTheme.caption)
              : Text(
                  _personEntry.person.name,
                )),
          DataCell(Text(_personEntry.person.email)),
          DataCell(Text('${_personEntry.person.groupCount}')),
          DataCell(Text(
              '${_personEntry.person.whenLastAuthenticated?.toLocal() ?? ""}')),

          if(_personEntry.person.whenDeactivated != null)
            DataCell(
              FHIconButton(
                tooltip: "Activate user",
                  icon: const Icon(Icons.restart_alt_sharp),
                onPressed: () =>
                    bloc.mrClient.addOverlay((BuildContext context) {
                      return FHAlertDialog(
                        title: Text("Activate user '${_personEntry.person.name}'"),
                        content:
                        Text('Are you sure you want to activate user with email address ${_personEntry.person.email}?'),
                        actions: [
                          TextButton(
                            onPressed: () {
                              bloc.mrClient.removeOverlay();
                            }, child: const Text("Cancel"),
                          ),
                          FHFlatButton(
                          title: 'OK',
                          onPressed:
                                () async {
                                  try {
                                    await bloc.activatePerson(
                                        _personEntry.person.id);
                                    setNextView(); // triggers reload from server with latest settings and rebuilds state
                                    bloc.mrClient.addSnackbar(Text(
                                        "User '${_personEntry.person
                                            .name}' activated!"));
                                  } catch (e, s) {
                                    await bloc.mrClient.dialogError(e, s);
                                  }
                          },
                        ),
                        ]
                      );
                    }),),
            ),
          if(_personEntry.person.whenDeactivated == null) DataCell(Row(children: <Widget>[
          Tooltip(
              message: _infoTooltip(_personEntry, allowedLocalIdentity),
              child: FHIconButton(
                icon: Icon(Icons.info,
                    color: _infoColour(_personEntry, allowedLocalIdentity)),
                onPressed: () =>
                    bloc.mrClient.addOverlay((BuildContext context) {
                  return ListUserInfoDialog(bloc, _personEntry);
                }),
              ),
            ),
            FHIconButton(
                icon: const Icon(Icons.edit),
                onPressed: () => {
                      ManagementRepositoryClientBloc.router
                          .navigateTo(context, '/manage-user', params: {
                        'id': [_personEntry.person.id]
                      })
                    }),
            // const SizedBox(
            //   width: 8.0,
            // ),
            FHIconButton(
              icon: const Icon(Icons.delete),
              onPressed: () => bloc.mrClient.addOverlay((BuildContext context) {
                return bloc.mrClient.person.id!.id == _personEntry.person.id
                    ? cantDeleteYourselfDialog(bloc)
                    : FHDeleteThingWarningWidget(
                        thing: "user '${_personEntry.person.name}'",
                        content:
                            'This user will be removed from all groups and deleted from the organization. \n\nThis cannot be undone!',
                        bloc: bloc.mrClient,
                        deleteSelected: () async {
                          try {
                            await bloc.deletePerson(
                                _personEntry.person.id, true);
                            setNextView(); // triggers reload from server with latest settings and rebuilds state
                            bloc.mrClient.addSnackbar(Text(
                                "User '${_personEntry.person.name}' deleted!"));
                            return true;
                          } catch (e, s) {
                            await bloc.mrClient.dialogError(e, s);
                            return false;
                          }
                        },
                      );
              }),
            ),
          ])),
        ],
        onSelectChanged: (newValue) {
          ManagementRepositoryClientBloc.router
              .navigateTo(context, '/manage-user', params: {
            'id': [_personEntry.person.id]
          });
        });
  }

  Widget cantDeleteYourselfDialog(ListUsersBloc bloc) {
    return FHAlertDialog(
      title: const Text("You can't delete yourself!"),
      content: const Text(
          "To delete yourself from the organization, you'll need to contact a site administrator."),
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

class ReactivateUserInfoDialog extends StatelessWidget {
  final ListUsersBloc bloc;
  final SearchPersonEntry entry;

  const ReactivateUserInfoDialog(this.bloc, this.entry, {Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHAlertDialog(
      title: const Text(
        'Activate user',
        style: TextStyle(fontSize: 22.0),
      ),
      content: Text(
        'Are you sure you want to activate user with email address ${entry.person.email}?',
        style: TextStyle(fontSize: 22.0),
      ),
      actions: <Widget>[
        // usually buttons at the bottom of the dialog
        FHFlatButton(
          title: 'OK',
          onPressed: () {
            bloc.mrClient.removeOverlay();
            bloc.activatePerson(entry.person.id);
          },
        )
      ],
    );
  }
}


class _ListUserInfo extends StatelessWidget {
  final ListUsersBloc bloc;
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
