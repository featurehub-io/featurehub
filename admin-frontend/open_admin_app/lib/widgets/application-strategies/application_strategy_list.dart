import 'package:advanced_datatable/advanced_datatable_source.dart';
import 'package:advanced_datatable/datatable.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/application-strategies/application_strategy_bloc.dart';
import 'package:open_admin_app/widgets/common/application_drop_down.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';

class ApplicationStrategyList extends StatefulWidget {
  const ApplicationStrategyList({Key? key}) : super(key: key);

  @override
  ApplicationStrategyListState createState() => ApplicationStrategyListState();
}

class ApplicationStrategyListState extends State<ApplicationStrategyList> {
  var sortIndex = 0;
  var sortAsc = true;
  var rowsPerPage = AdvancedPaginatedDataTable.defaultRowsPerPage;

  late ApplicationStrategyDataTableSource source;
  late ApplicationStrategyBloc bloc;

  @override
  void initState() {
    super.initState();
    bloc = BlocProvider.of<ApplicationStrategyBloc>(context);
    source = ApplicationStrategyDataTableSource(bloc, context);
  }

  @override
  Widget build(BuildContext context) {
    final debouncer = Debouncer(milliseconds: 500);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        StreamBuilder<List<Application>?>(
            stream: bloc.currentApplicationsStream,
            builder: (context, snapshot) {
              if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Padding(
                      padding: const EdgeInsets.symmetric(
                          vertical: 16.0, horizontal: 8.0),
                      child: Wrap(
                        spacing: 16.0,
                        runSpacing: 16.0,
                        children: [
                          Row(
                            children: [
                              ApplicationDropDown(
                                  applications: snapshot.data!, bloc: bloc),
                              const SizedBox(
                                width: 16.0,
                              ),
                              if (bloc.appId != null &&
                                  bloc.mrClient
                                      .userHasAppStrategyCreationRoleInCurrentApplication)
                                FilledButton.icon(
                                  icon: const Icon(Icons.add),
                                  label: const Text('Create new strategy'),
                                  onPressed: () {
                                    ManagementRepositoryClientBloc.router
                                        .navigateTo(context,
                                            '/create-application-strategy',
                                            params: {
                                          'appid': [bloc.appId ?? ""]
                                        });
                                  },
                                )
                            ],
                          ),
                        ],
                      ),
                    ),
                    const FHPageDivider(),
                    const SizedBox(height: 16.0),
                    Container(
                        constraints: const BoxConstraints(maxWidth: 300),
                        child: TextField(
                          decoration: const InputDecoration(
                            hintText: 'Search strategy',
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
                        showCheckboxColumn: false,
                        addEmptyRows: false,
                        sortAscending: sortAsc,
                        sortColumnIndex: sortIndex,
                        customTableFooter: (source, offset) {
                          return const Text("");
                        },
                        showHorizontalScrollbarAlways: false,
                        columns: [
                          DataColumn(
                              label: const Text('Name'), onSort: setSort),
                          const DataColumn(
                            label: Text("Date created (UTC)"),
                          ),
                          const DataColumn(label: Text("Date updated (UTC)")),
                          const DataColumn(label: Text("Created by")),
                          const DataColumn(
                            label: Text('Used in'),
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
                    )
                  ],
                );
              }
              if (snapshot.hasData && snapshot.data!.isEmpty) {
                return StreamBuilder<ReleasedPortfolio?>(
                    stream: bloc.mrClient.streamValley.currentPortfolioStream,
                    builder: (context, snapshot) {
                      if (snapshot.hasData &&
                          snapshot.data!.currentPortfolioOrSuperAdmin) {
                        return Row(
                          children: <Widget>[
                            SelectableText(
                                'Cannot create application strategy as there are no applications in this portfolio',
                                style: Theme.of(context).textTheme.bodySmall),
                          ],
                        );
                      } else {
                        return SelectableText(
                            "Either there are no applications in this portfolio or you don't have access to any of the applications.\n"
                            'Please contact your administrator.',
                            style: Theme.of(context).textTheme.bodySmall);
                      }
                    });
              }
              return const SizedBox.shrink();
            }),
      ],
    );
  }

  void setSort(int i, bool asc) => setState(() {
        sortIndex = i;
        sortAsc = asc;
      });
}

class ApplicationStrategyDataTableSource
    extends AdvancedDataTableSource<ListApplicationRolloutStrategyItem> {
  String lastSearchTerm = '';
  final ApplicationStrategyBloc bloc;
  final BuildContext context;

  ApplicationStrategyDataTableSource(this.bloc, this.context) {
    bloc.mrClient.streamValley.currentAppIdStream.listen((value) {
      setNextView();
    });
  }

  @override
  bool get isRowCountApproximate => false;

  @override
  int get selectedRowCount => 0;

  void filterServerSide(String filterQuery) {
    lastSearchTerm = filterQuery.toLowerCase().trim();
    setNextView();
  }

  @override
  Future<RemoteDataSourceDetails<ListApplicationRolloutStrategyItem>>
      getNextPage(NextPageRequest pageRequest) async {
    print("sort order " + pageRequest.sortAscending.toString());
    final data = await bloc.getStrategiesData(
        lastSearchTerm.isNotEmpty ? lastSearchTerm : null,
        (pageRequest.sortAscending ?? true) == true
            ? SortOrder.ASC
            : SortOrder.DESC);
    List<ListApplicationRolloutStrategyItem> rs = data.items;
    return RemoteDataSourceDetails(
      data.max,
      rs,
      filteredRows: null, //the total amount of filtered rows, null by default
    );
  }

  @override
  DataRow getRow(int index) {
    final strategy = lastDetails!.rows[index];
    return DataRow.byIndex(
        index: index,
        cells: [
          DataCell(Text(
            strategy.strategy.name,
          )),
          DataCell(Text(
            DateFormat('yyyy-MM-dd HH:mm:ss').format(strategy.whenCreated),
          )),
          DataCell(Text(
            DateFormat('yyyy-MM-dd HH:mm:ss').format(strategy.whenUpdated),
          )),
          DataCell(
            Text(strategy.updatedBy.email),
          ),
          DataCell(Text(
              'environments: ${strategy.usage!.length}, feature values: ${strategy.usage!.map((e) => e.featuresCount).sum}')),
          DataCell(Row(children: <Widget>[
            if (bloc.mrClient.userHasAppStrategyEditRoleInCurrentApplication)
              FHIconButton(
                  icon: const Icon(Icons.edit),
                  onPressed: () => {
                        ManagementRepositoryClientBloc.router.navigateTo(
                            context, '/edit-application-strategy',
                            params: {
                              'id': [strategy.strategy.id],
                              'appid': [bloc.appId ?? ""]
                            })
                      }),
            // const SizedBox(
            //   width: 8.0,
            // ),
            if (bloc.mrClient.userHasAppStrategyEditRoleInCurrentApplication)
              FHIconButton(
                icon: const Icon(Icons.delete),
                onPressed: () =>
                    bloc.mrClient.addOverlay((BuildContext context) {
                  return FHDeleteThingWarningWidget(
                    thing: "Application strategy '${strategy.strategy.name}'",
                    content:
                        'This application strategy will be deleted and unassigned from all the flags. \n\nThis cannot be undone!',
                    bloc: bloc.mrClient,
                    deleteSelected: () async {
                      try {
                        await bloc.deleteStrategy(strategy.strategy.id);
                        setNextView(); // triggers reload from server with latest settings and rebuilds state
                        bloc.mrClient.addSnackbar(Text(
                            "Application strategy '${strategy.strategy.name}' deleted!"));
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
          if (bloc.mrClient.userHasAppStrategyEditRoleInCurrentApplication) {
            ManagementRepositoryClientBloc.router
                .navigateTo(context, '/edit-application-strategy', params: {
              'id': [strategy.strategy.id],
              'appid': [bloc.appId ?? ""]
            });
          }
        });
  }
}
