import 'package:advanced_datatable/advanced_datatable_source.dart';
import 'package:advanced_datatable/datatable.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/utils/utils.dart';
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
  var sortIndex = 0;
  var sortAsc = true;
  var rowsPerPage = AdvancedPaginatedDataTable.defaultRowsPerPage;

  late AdminServiceAccountDataTableSource source;
  late ListUsersBloc bloc;

  @override
  void initState() {
    super.initState();
    bloc = BlocProvider.of<ListUsersBloc>(context);
    source = AdminServiceAccountDataTableSource(bloc, context);
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
              decoration: const InputDecoration(
                hintText: 'Search Service Accounts',
                icon: Icon(Icons.search),
              ),
              onChanged: (val) {
                debouncer.run(
                  () {
                    source.filterServerSide(val);
                  },
                );
              },
            )),
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
            showHorizontalScrollbarAlways: true,
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
                label: Text('Groups'),
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

class AdminServiceAccountDataTableSource
    extends AdvancedDataTableSource<SearchPerson> {
  String lastSearchTerm = '';
  final ListUsersBloc bloc;
  final BuildContext context;

  AdminServiceAccountDataTableSource(this.bloc, this.context);

  @override
  bool get isRowCountApproximate => false;

  @override
  int get selectedRowCount => 0;

  void filterServerSide(String filterQuery) {
    lastSearchTerm = filterQuery.toLowerCase().trim();
    setNextView();
  }

  @override
  Future<RemoteDataSourceDetails<SearchPerson>> getNextPage(
      NextPageRequest pageRequest) async {
    final data = await bloc.findPeople(
      pageRequest.pageSize,
      pageRequest.offset,
      lastSearchTerm.isNotEmpty ? lastSearchTerm : null,
      (pageRequest.sortAscending ?? true) == true
          ? SortOrder.ASC
          : SortOrder.DESC,
      PersonType.serviceAccount,
    );
    final userList = bloc.transformAdminApiKeys(data);
    return RemoteDataSourceDetails(
      data.max,
      userList.toList(),
      filteredRows: null, //the total amount of filtered rows, null by default
    );
  }

  @override
  DataRow getRow(int index) {
    final serviceAccount = lastDetails!.rows[index];
    return DataRow.byIndex(
        index: index,
        cells: [
          DataCell(Text(
            serviceAccount.name,
          )),
          DataCell(Text('${serviceAccount.groupCount}')),
          DataCell(Row(children: <Widget>[
            FHIconButton(
              icon: const Icon(Icons.info),
              onPressed: () => bloc.mrClient.addOverlay((BuildContext context) {
                return ServiceAccountInfoDialog(bloc, serviceAccount);
              }),
            ),
            FHIconButton(
                icon: const Icon(Icons.edit),
                onPressed: () => {
                      ManagementRepositoryClientBloc.router.navigateTo(
                          context, '/edit-admin-service-account',
                          params: {
                            'id': [serviceAccount.id]
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
              onPressed: () => bloc.mrClient.addOverlay((BuildContext context) {
                return AdminSAKeyResetDialogWidget(
                  person: serviceAccount,
                  bloc: bloc,
                );
              }),
            ),
            FHIconButton(
              icon: const Icon(Icons.delete),
              onPressed: () => bloc.mrClient.addOverlay((BuildContext context) {
                return FHDeleteThingWarningWidget(
                  thing: "service account '${serviceAccount.name}'",
                  content:
                      'This service account will be removed from all groups and deleted from the organization. \n\nThis cannot be undone!',
                  bloc: bloc.mrClient,
                  deleteSelected: () async {
                    try {
                      await bloc.deletePerson(serviceAccount.id, true);
                      setNextView(); // triggers reload from server with latest settings and rebuilds state
                      bloc.mrClient.addSnackbar(Text(
                          "Service account '${serviceAccount.name}' deleted!"));
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
              .navigateTo(context, '/edit-admin-service-account', params: {
            'id': [serviceAccount.id]
          });
        });
  }
}

class ServiceAccountInfoDialog extends StatelessWidget {
  final ListUsersBloc bloc;
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
  final ListUsersBloc bloc;
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
                height: 400.0,
                width: 400.0,
                child: ListView(
                  children: [
                    _AdminServiceAccountRow(
                      title: 'Name',
                      child: Text(entry.name!,
                          style: Theme.of(context).textTheme.bodyLarge),
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
                              style: Theme.of(context).textTheme.bodySmall,
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
                                                    .bodyMedium,
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
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ),
        Expanded(flex: 5, child: child)
      ],
    );
  }
}
