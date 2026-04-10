import 'dart:async';

import 'package:advanced_datatable/advanced_datatable_source.dart';
import 'package:advanced_datatable/datatable.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
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
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/user/list/list_users_bloc.dart';

class PersonListWidget extends StatefulWidget {
  const PersonListWidget({super.key});

  @override
  PersonListWidgetState createState() => PersonListWidgetState();
}

class PersonListWidgetState extends State<PersonListWidget> {
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
    final debouncer = Debouncer(milliseconds: 500);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Container(
          constraints: const BoxConstraints(maxWidth: 300),
          child: TextField(
            decoration: InputDecoration(
              hintText: AppLocalizations.of(context)!.searchUsers,
              icon: const Icon(Icons.search),
            ),
            onChanged: (val) {
              debouncer.run(() {
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
            showHorizontalScrollbarAlways: true,
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
              DataColumn(
                  label: Text(AppLocalizations.of(context)!.columnName),
                  onSort: setSort),
              DataColumn(
                  label: Text(AppLocalizations.of(context)!.columnStatus),
                  onSort: setSort),
              DataColumn(
                  label: Text(AppLocalizations.of(context)!.columnEmail)),
              DataColumn(label: Text(AppLocalizations.of(context)!.groups)),
              DataColumn(
                  label: Text(AppLocalizations.of(context)!.columnLastSignIn)),
              DataColumn(
                label: Padding(
                  padding: const EdgeInsets.only(left: 12.0),
                  child: Text(AppLocalizations.of(context)!.columnActions),
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
        PersonType.person,
        includeDeactivated: true,
        sortBy: pageRequest.columnSortIndex == 1
            ? SearchPersonSortBy.activationStatus
            : SearchPersonSortBy.name);
    final userList = bloc.transformPeople(data);
    return RemoteDataSourceDetails(
      data.max,
      userList.toList(),
      filteredRows: null, //the total amount of filtered rows, null by default
    );
  }

  @override
  DataRow getRow(int index) {
    final personEntry = lastDetails!.rows[index];
    final allowedLocalIdentity = bloc.mrClient.identityProviders.hasLocal;
    return DataRow.byIndex(
        index: index,
        cells: [
          DataCell(personEntry.person.name == "No name"
              ? Text(AppLocalizations.of(context)!.notYetRegistered,
                  style: Theme.of(context).textTheme.bodySmall)
              : Text(
                  personEntry.person.name,
                )),
          DataCell(Text(personEntry.person.whenDeactivated != null
              ? AppLocalizations.of(context)!.statusDeactivated
              : AppLocalizations.of(context)!.statusActive)),
          DataCell(Text(personEntry.person.email)),
          DataCell(Text('${personEntry.person.groupCount}')),
          DataCell(personEntry.person.whenLastAuthenticated != null
              ? Text(DateFormat('yyyy-MM-dd HH:mm:ss')
                  .format(personEntry.person.whenLastAuthenticated!))
              : const Text("")),
          if (personEntry.person.whenDeactivated != null)
            DataCell(
              FHIconButton(
                tooltip: AppLocalizations.of(context)!.activateUserTooltip,
                icon: const Icon(
                  Icons.restart_alt_sharp,
                  color: Colors.red,
                ),
                onPressed: () =>
                    bloc.mrClient.addOverlay((BuildContext context) {
                  final l10n = AppLocalizations.of(context)!;
                  return FHAlertDialog(
                      title:
                          Text(l10n.activateUserTitle(personEntry.person.name)),
                      content: Text(
                          l10n.activateUserConfirm(personEntry.person.email)),
                      actions: [
                        TextButton(
                          onPressed: () {
                            bloc.mrClient.removeOverlay();
                          },
                          child: Text(l10n.cancel),
                        ),
                        FHFlatButton(
                          title: l10n.activate,
                          onPressed: () async {
                            try {
                              await bloc.activatePerson(personEntry.person);
                              setNextView(); // triggers reload from server with latest settings and rebuilds state
                              bloc.mrClient.addSnackbar(Text(
                                  l10n.userActivated(personEntry.person.name)));
                              bloc.mrClient.removeOverlay();
                            } catch (e, s) {
                              bloc.mrClient.removeOverlay();
                              bloc.mrClient.dialogError(e, s);
                            }
                          },
                        ),
                      ]);
                }),
              ),
            ),
          if (personEntry.person.whenDeactivated == null)
            DataCell(Row(children: <Widget>[
              Tooltip(
                message: _infoTooltip(personEntry, allowedLocalIdentity,
                    AppLocalizations.of(context)!),
                child: FHIconButton(
                  icon: Icon(Icons.info,
                      color: _infoColour(personEntry, allowedLocalIdentity)),
                  onPressed: () =>
                      bloc.mrClient.addOverlay((BuildContext context) {
                    return ListUserInfoDialog(bloc, personEntry);
                  }),
                ),
              ),
              FHIconButton(
                  icon: const Icon(Icons.edit),
                  onPressed: () => {
                        ManagementRepositoryClientBloc.router
                            .navigateTo(context, '/manage-user', params: {
                          'id': [personEntry.person.id]
                        })
                      }),
              // const SizedBox(
              //   width: 8.0,
              // ),
              FHIconButton(
                icon: const Icon(Icons.delete),
                onPressed: () =>
                    bloc.mrClient.addOverlay((BuildContext context) {
                  return bloc.mrClient.person.id!.id == personEntry.person.id
                      ? cantDeleteYourselfDialog(bloc)
                      : FHDeleteThingWarningWidget(
                          thing: "user '${personEntry.person.name}'",
                          content:
                              AppLocalizations.of(context)!.deleteUserContent,
                          bloc: bloc.mrClient,
                          deleteSelected: () async {
                            try {
                              await bloc.deletePerson(
                                  personEntry.person.id, true);
                              setNextView(); // triggers reload from server with latest settings and rebuilds state
                              if (context.mounted) {
                                bloc.mrClient.addSnackbar(Text(
                                    AppLocalizations.of(context)!
                                        .userDeactivated(
                                            personEntry.person.name)));
                              }
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
          if (personEntry.person.whenDeactivated == null) {
            ManagementRepositoryClientBloc.router
                .navigateTo(context, '/manage-user', params: {
              'id': [personEntry.person.id]
            });
          } else {
            null;
          }
        });
  }

  Widget cantDeleteYourselfDialog(ListUsersBloc bloc) {
    final l10n = AppLocalizations.of(context)!;
    return FHAlertDialog(
      title: Text(l10n.cantDeleteYourself),
      content: Text(l10n.cantDeleteYourselfContent),
      actions: <Widget>[
        FHFlatButton(
          title: l10n.ok,
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

  const ListUserInfoDialog(this.bloc, this.entry, {super.key});

  @override
  Widget build(BuildContext context) {
    return FHAlertDialog(
      title: Text(
        AppLocalizations.of(context)!.userInformation,
        style: const TextStyle(fontSize: 22.0),
      ),
      content: _ListUserInfo(bloc: bloc, foundPerson: entry),
      actions: <Widget>[
        FHFlatButton(
          title: AppLocalizations.of(context)!.ok,
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
  final SearchPersonEntry foundPerson;

  const _ListUserInfo({required this.bloc, required this.foundPerson});

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
                      title: AppLocalizations.of(context)!.columnName,
                      child: Text(foundPerson.person.name,
                          style: Theme.of(context).textTheme.bodyLarge),
                    ),
                    const SizedBox(height: 8),
                    _ListUserRow(
                      title: AppLocalizations.of(context)!.columnEmail,
                      child: Text(foundPerson.person.email,
                          style: Theme.of(context).textTheme.bodyLarge),
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
                              AppLocalizations.of(context)!.registrationUrl,
                              style: Theme.of(context).textTheme.bodySmall,
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
                            tooltipMessage: AppLocalizations.of(context)!
                                .copyUrlToClipboard,
                            copyString: bloc.mrClient.registrationUrl(
                                foundPerson.registration.token),
                          )
                        ],
                      ),
                    if (allowedLocalIdentity &&
                        foundPerson.registration.expired)
                      Padding(
                        padding: const EdgeInsets.only(top: 12.0, bottom: 4.0),
                        child: Text(
                          AppLocalizations.of(context)!.registrationExpired,
                          style: const TextStyle(fontWeight: FontWeight.bold),
                        ),
                      ),
                    if (allowedLocalIdentity &&
                        foundPerson.registration.expired)
                      FHCopyToClipboardFlatButton(
                        caption:
                            AppLocalizations.of(context)!.renewRegistrationUrl,
                        textProvider: () async {
                          try {
                            final token = await bloc.mrClient.authServiceApi
                                .resetExpiredToken(foundPerson.person.email);
                            bloc.mrClient.addSnackbar(Text(
                                AppLocalizations.of(context)!
                                    .registrationUrlRenewed));
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
                              AppLocalizations.of(context)!.groups,
                              style: Theme.of(context).textTheme.bodySmall,
                            ),
                          ),
                          Expanded(
                              flex: 5,
                              child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    if (person.groups.isNotEmpty)
                                      ...person.groups.map((e) => Text(
                                            e.name,
                                            style: Theme.of(context)
                                                .textTheme
                                                .bodyMedium,
                                          )),
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

  const _ListUserRow({required this.title, required this.child});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          flex: 1,
          child: Text(
            title,
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ),
        Expanded(flex: 5, child: child)
      ],
    );
  }
}

String _infoTooltip(
    SearchPersonEntry entry, bool allowedLocalLogin, AppLocalizations l10n) {
  if (entry.registration.expired) {
    return l10n.registrationExpired;
  }
  return "";
}

Color _infoColour(SearchPersonEntry entry, bool allowedLocalLogin) {
  if (entry.registration.expired) {
    return Colors.red;
  }

  return Colors.green;
}
