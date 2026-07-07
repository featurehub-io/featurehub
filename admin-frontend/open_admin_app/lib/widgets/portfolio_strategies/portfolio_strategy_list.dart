import 'package:advanced_datatable/advanced_datatable_source.dart';
import 'package:advanced_datatable/datatable.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';
import 'package:open_admin_app/widgets/portfolio_strategies/portfolio_strategy_bloc.dart';

class PortfolioStrategyList extends StatefulWidget {
  const PortfolioStrategyList({super.key});

  @override
  PortfolioStrategyListState createState() => PortfolioStrategyListState();
}

class PortfolioStrategyListState extends State<PortfolioStrategyList> {
  var sortIndex = 0;
  var sortAsc = true;
  var rowsPerPage = AdvancedPaginatedDataTable.defaultRowsPerPage;

  late PortfolioStrategyDataTableSource source;
  late PortfolioStrategyBloc bloc;

  @override
  void initState() {
    super.initState();
    bloc = BlocProvider.of<PortfolioStrategyBloc>(context);
    source = PortfolioStrategyDataTableSource(bloc, context);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final debouncer = Debouncer(milliseconds: 500);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        StreamBuilder<String?>(
            stream: bloc.currentPortfolioIdStream,
            builder: (context, snapshot) {
              if (snapshot.hasData && snapshot.data != null) {
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Padding(
                      padding: const EdgeInsets.symmetric(
                          vertical: 16.0, horizontal: 8.0),
                      child: StreamBuilder<ReleasedPortfolio?>(
                          stream: bloc.mrClient.streamValley
                              .currentPortfolioStream,
                          builder: (context, portfolioSnapshot) {
                            final canCreate = portfolioSnapshot.hasData &&
                                (portfolioSnapshot
                                        .data!.currentPortfolioStrategyEditCreate ||
                                    portfolioSnapshot
                                        .data!.currentPortfolioOrSuperAdmin);
                            return Row(
                              children: [
                                if (canCreate)
                                  FilledButton.icon(
                                    icon: const Icon(Icons.add),
                                    label: Text(l10n.createNewStrategy),
                                    onPressed: () {
                                      ManagementRepositoryClientBloc.router
                                          .navigateTo(context,
                                              '/create-portfolio-strategy',
                                              params: {
                                            'pid': [bloc.portfolioId ?? '']
                                          });
                                    },
                                  ),
                              ],
                            );
                          }),
                    ),
                    const FHPageDivider(),
                    const SizedBox(height: 16.0),
                    Container(
                        constraints: const BoxConstraints(maxWidth: 300),
                        child: TextField(
                          decoration: InputDecoration(
                            hintText: l10n.searchStrategy,
                            icon: const Icon(Icons.search),
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
                          return const Text('');
                        },
                        showHorizontalScrollbarAlways: false,
                        columns: [
                          DataColumn(
                              label: Text(l10n.columnStrategyName),
                              onSort: setSort),
                          DataColumn(
                            label: Text(l10n.columnDateCreated),
                          ),
                          DataColumn(label: Text(l10n.columnDateUpdated)),
                          DataColumn(label: Text(l10n.columnCreatedBy)),
                          DataColumn(
                            label: Text(l10n.columnUsedIn),
                          ),
                          DataColumn(
                            label: Padding(
                              padding: const EdgeInsets.only(left: 12.0),
                              child: Text(l10n.columnActions),
                            ),
                          ),
                        ],
                        source: source,
                      ),
                    )
                  ],
                );
              }
              return StreamBuilder<ReleasedPortfolio?>(
                  stream: bloc.mrClient.streamValley.currentPortfolioStream,
                  builder: (context, snapshot) {
                    if (snapshot.hasData && snapshot.data!.isNull()) {
                      return SelectableText(
                          l10n.noApplicationsAccessMessage,
                          style: Theme.of(context).textTheme.bodySmall);
                    }
                    return const SizedBox.shrink();
                  });
            }),
      ],
    );
  }

  void setSort(int i, bool asc) => setState(() {
        sortIndex = i;
        sortAsc = asc;
      });
}

class PortfolioStrategyDataTableSource
    extends AdvancedDataTableSource<ListPortfolioRolloutStrategyItem> {
  String lastSearchTerm = '';
  final PortfolioStrategyBloc bloc;
  final BuildContext context;

  PortfolioStrategyDataTableSource(this.bloc, this.context) {
    bloc.mrClient.streamValley.currentPortfolioIdStream.listen((value) {
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
  Future<RemoteDataSourceDetails<ListPortfolioRolloutStrategyItem>>
      getNextPage(NextPageRequest pageRequest) async {
    final data = await bloc.getStrategiesData(
        lastSearchTerm.isNotEmpty ? lastSearchTerm : null,
        (pageRequest.sortAscending ?? true) == true
            ? SortOrder.ASC
            : SortOrder.DESC);
    List<ListPortfolioRolloutStrategyItem> rs = data.items;
    return RemoteDataSourceDetails(
      data.max,
      rs,
      filteredRows: null,
    );
  }

  @override
  DataRow getRow(int index) {
    final strategy = lastDetails!.rows[index];
    final l10n = AppLocalizations.of(context)!;
    final usage = strategy.usage ?? [];
    final envCount = usage.length;
    final featureCount = usage.map((e) => e.featuresCount).sum;

    return DataRow.byIndex(
        index: index,
        cells: [
          DataCell(Text(strategy.strategy.name)),
          DataCell(Text(
            DateFormat('yyyy-MM-dd HH:mm:ss').format(strategy.whenCreated),
          )),
          DataCell(Text(
            DateFormat('yyyy-MM-dd HH:mm:ss').format(strategy.whenUpdated),
          )),
          DataCell(
            Text(strategy.updatedBy.email),
          ),
          DataCell(Text(l10n.strategyUsage(envCount, featureCount))),
          DataCell(Row(children: <Widget>[
            StreamBuilder<ReleasedPortfolio?>(
                stream: bloc.mrClient.streamValley.currentPortfolioStream,
                builder: (context, portfolioSnapshot) {
                  final canEdit = portfolioSnapshot.hasData &&
                      (portfolioSnapshot
                              .data!.currentPortfolioStrategyEditCreate ||
                          portfolioSnapshot
                              .data!.currentPortfolioStrategyEditor ||
                          portfolioSnapshot
                              .data!.currentPortfolioOrSuperAdmin);
                  if (!canEdit) return const SizedBox.shrink();
                  return Row(children: [
                    FHIconButton(
                        icon: const Icon(Icons.edit),
                        onPressed: () => {
                              ManagementRepositoryClientBloc.router.navigateTo(
                                  context, '/edit-portfolio-strategy',
                                  params: {
                                    'id': [strategy.strategy.id],
                                    'pid': [bloc.portfolioId ?? '']
                                  })
                            }),
                    FHIconButton(
                      icon: const Icon(Icons.delete),
                      onPressed: () =>
                          bloc.mrClient.addOverlay((BuildContext context) {
                        return FHDeleteThingWarningWidget(
                          thing:
                              "Portfolio strategy '${strategy.strategy.name}'",
                          content: l10n.portfolioStrategyDeleteContent,
                          bloc: bloc.mrClient,
                          deleteSelected: () async {
                            try {
                              await bloc.deleteStrategy(strategy.strategy.id);
                              setNextView();
                              bloc.mrClient.addSnackbar(Text(
                                  l10n.portfolioStrategyDeleted(
                                      strategy.strategy.name)));
                              return true;
                            } catch (e, s) {
                              await bloc.mrClient.dialogError(e, s);
                              return false;
                            }
                          },
                        );
                      }),
                    ),
                  ]);
                }),
          ])),
        ],
        onSelectChanged: (newValue) {
          final rel = bloc.mrClient.streamValley.currentPortfolio;
          final canEdit = rel.currentPortfolioStrategyEditCreate ||
              rel.currentPortfolioStrategyEditor ||
              rel.currentPortfolioOrSuperAdmin;
          if (canEdit) {
            ManagementRepositoryClientBloc.router
                .navigateTo(context, '/edit-portfolio-strategy', params: {
              'id': [strategy.strategy.id],
              'pid': [bloc.portfolioId ?? '']
            });
          }
        });
  }
}