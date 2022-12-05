import 'package:async/async.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import 'package:mrapi/api.dart';
import 'package:multiselect/multiselect.dart';
import 'package:open_admin_app/routes/features_overview_route_v2.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/features/feature_cell_holder.dart';

import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';
import 'package:open_admin_app/widgets/features/table-collapsed-view/value_cellV2.dart';
import 'package:syncfusion_flutter_datagrid/datagrid.dart';

import 'feature_dashboard_constants.dart';

class ExperimentTable extends StatefulWidget {
  ExperimentTable(
      {Key? key, this.title, required this.bloc, required this.data})
      : super(key: key);
  final String? title;
  final PerApplicationFeaturesBloc bloc;
  final FeatureStatusFeatures data;

  @override
  _ExperimentTableState createState() => _ExperimentTableState();
}

class _ExperimentTableState extends State<ExperimentTable> {
  late FeaturesDataSource _featuresDataSource;
  List<FeatureStatusFeatures> featuresList = [];
  final AsyncMemoizer _memoizer = AsyncMemoizer();

  String _searchTerm = '';
  int _maxFeatures = 0;
  int _pageOffset = 0;

  List<FeatureValueType> _selectedFeatureTypes = [];
  List<String> _selectedEnvironmentList = [];

  Future generateFeaturesList() async {
    print("generate features list");
    var appFeatures = await widget.bloc.getApplicationFeatureValuesData(
        widget.bloc.applicationId!,
        _searchTerm,
        _selectedFeatureTypes, rowsPerPage, _pageOffset); //handle if appId is null
    var featuresList = FeatureStatusFeatures(appFeatures);
    _featuresDataSource = FeaturesDataSource(featuresList);
    _maxFeatures = appFeatures.maxFeatures;
    return featuresList;
  }

//   return this._memoizer.runOnce(() async {
//   print("generate features list");
//   var appFeatures = await widget.bloc.getApplicationFeatureValuesData(
//   widget.bloc.applicationId!,
//   _searchTerm,
//   _selectedFeatureTypes,
//   rowsPerPage,
//   _pageOffset); //handle if appId is null
//   var featuresList = FeatureStatusFeatures(appFeatures);
//   _featuresDataSource = FeaturesDataSource(featuresList);
//   _maxFeatures = appFeatures.maxFeatures;
//   return featuresList;
// });

      @override
      void initState() {
        super.initState();
        print("in init state");
        _selectedEnvironmentList = widget.data.applicationEnvironments.entries
            .map((e) => e.value.environmentName!)
            .toList();
      }

      final CustomColumnSizer _customColumnSizer = CustomColumnSizer();

      @override
      Widget build(BuildContext context) {
        final _debouncer = Debouncer(milliseconds: 500);

        var gridColumnsList = widget.data.applicationEnvironments.entries
            .map(
              (entry) => GridColumn(
            columnName: "env",
            label: Container(
                padding: const EdgeInsets.all(8.0),
                alignment: Alignment.center,
                child: Text(entry.value.environmentName!)),
            visible:
            _selectedEnvironmentList.contains(entry.value.environmentName),
          ),
        )
            .toList();

        return FutureBuilder(
            future: generateFeaturesList(),
            builder: (context, snapshot) {
              print("rebuild");
              return snapshot.hasData
                  ? Column(
                mainAxisAlignment: MainAxisAlignment.start,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      CreateFeatureButton(
                        bloc: widget.bloc,
                        featuresDataSource: _featuresDataSource,
                      ),
                      const SizedBox(
                        width: 16.0,
                      ),
                      Container(
                        constraints: const BoxConstraints(maxWidth: 300),
                        child: TextField(
                          decoration: const InputDecoration(
                            hintText: 'Search features',
                            icon: Icon(Icons.search),
                          ),
                          onChanged: (val) {
                            _debouncer.run(() {
                              setState(() {
                                _searchTerm = val;
                              });
                            });
                          },
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 24.0),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.start,
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Container(
                        constraints: const BoxConstraints(
                            maxWidth: 600, maxHeight: 70),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text("Select environments to display",
                                style: Theme.of(context).textTheme.caption),
                            SizedBox(height: 8.0),
                            DropDownMultiSelect(
                                hint: Text("Select environments to display",
                                    style: Theme.of(context)
                                        .textTheme
                                        .bodyMedium),
                                onChanged: (List<String> x) {
                                  setState(() {
                                    _selectedEnvironmentList = x;
                                  });
                                },
                                icon: const Icon(
                                  Icons.visibility_sharp,
                                  size: 18,
                                ),
                                options: widget
                                    .data.applicationEnvironments.entries
                                    .map((e) => e.value.environmentName!)
                                    .toList(),
                                selectedValues: _selectedEnvironmentList
                              // whenEmpty: 'Select Something',
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 16),
                      Container(
                        constraints: const BoxConstraints(maxWidth: 300),
                        child: DropDownMultiSelect(
                          icon: const Icon(
                            Icons.filter_alt,
                            size: 18,
                          ),
                          hint: const Text("Filter by feature type"),
                          onChanged: (List<String> x) {
                            setState(() {
                              _selectedFeatureTypes = x
                                  .map((e) => convertToFeatureValueType(e))
                                  .toList();
                            });
                          },
                          options: FeatureValueType.values
                              .map((e) => e.name!)
                              .toList(),
                          selectedValues: _selectedFeatureTypes
                              .map((e) => e.name!)
                              .toList(),
                          // whenEmpty: 'Select Something',
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16.0),
                  Container(
                    height: 600,
                    child: SfDataGrid(
                      source: _featuresDataSource,
                      // columnWidthMode: ColumnWidthMode.lastColumnFill,
                      isScrollbarAlwaysShown: true,
                      // allowSorting: true,
                      rowsPerPage: rowsPerPage,
                      defaultColumnWidth: 150,
                      columnSizer: _customColumnSizer,
                      frozenColumnsCount: 1,
                      onQueryRowHeight: (details) {
                        return details
                            .getIntrinsicRowHeight(details.rowIndex);
                      },
                      columns: <GridColumn>[
                        GridColumn(
                          minimumWidth: 200,
                          columnName: 'features name',
                          label: Container(
                              padding: const EdgeInsets.all(16.0),
                              alignment: Alignment.center,
                              child: const Text(
                                "Features name",
                              )),
                        ),
                        ...gridColumnsList,
                      ],
                    ),
                  ),
                  if (_maxFeatures >
                      rowsPerPage) // only display paginator if needed
                    Container(
                      // height: _dataPagerHeight,
                        child: SfDataPager(
                          delegate: _featuresDataSource,
                          pageCount:
                          (_maxFeatures / rowsPerPage).ceil().ceilToDouble(),
                          direction: Axis.horizontal,
                          onPageNavigationEnd: (page) {
                            _pageOffset=page;
                          },
                        ))
                ],
              )
                  : const Center(
                child: CircularProgressIndicator(
                  strokeWidth: 3,
                ),
              );
            });
      }
    }

        FeatureValueType convertToFeatureValueType(String value) {
      switch (value) {
        case "BOOLEAN":
          return FeatureValueType.BOOLEAN;
        case "STRING":
          return FeatureValueType.STRING;
        case "NUMBER":
          return FeatureValueType.NUMBER;
        case "JSON":
          return FeatureValueType.JSON;
      }
      return FeatureValueType.BOOLEAN;
    }

class AggregatedFeatureCellData {
  final EnvironmentFeatureValues efv;
  final ApplicationFeatureValues afv;
  final Feature feature;
  final FeatureValue? fv;

  AggregatedFeatureCellData(
      {required this.afv, required this.efv, required this.feature, this.fv});
}

class FeaturesDataSource extends DataGridSource {
  /// Creates the data source class with required details.
  final FeatureStatusFeatures data;

  FeaturesDataSource(this.data) {
    buildDataGridRows();
  }

  void buildDataGridRows() {
    List<Feature> featuresListExtracted =
        data.applicationFeatureValues.features;
    Map<String, EnvironmentFeatureValues> efvMap = data.applicationEnvironments;
    _featuresData = featuresListExtracted.map<DataGridRow>((feature) {
      var _cells = efvMap.entries
          .map((entry) => DataGridCell<AggregatedFeatureCellData>(
              columnName: entry.key,
              value: AggregatedFeatureCellData(
                  afv: data.applicationFeatureValues,
                  efv: entry.value,
                  feature: feature,
                  fv: entry.value.features
                      .firstWhere((fv) => fv.key == feature.key, orElse: () {
                    return FeatureValue(key: feature.key!, locked: false);
                  }))))
          .toList();
      return DataGridRow(cells: [
        DataGridCell<Feature>(columnName: 'feature name', value: feature),
        ..._cells,
      ]);
    }).toList();
  }

  // @override
  // Future<bool> handlePageChange(int oldPageIndex, int newPageIndex) async {
  //   if(oldPageIndex != newPageIndex) {
  //     // need to fix this function
  //     // buildDataGridRows();
  //     // updateDataGridSource();
  //     // notifyDataSourceListeners();
  //     return Future<bool>.value(true);
  //   }
  //   return Future<bool>.value(false);
  //
  // }

  List<DataGridRow> _featuresData = [];

  @override
  List<DataGridRow> get rows => _featuresData;

  @override
  DataGridRowAdapter buildRow(DataGridRow row) {
    return DataGridRowAdapter(
        cells: row.getCells().map<Widget>((dataGridCell) {
      if (dataGridCell.columnName == "feature name") {
        Feature feature = dataGridCell.value;
        return FeatureCellHolder(feature: feature); // adapt feature column
      } else {
        AggregatedFeatureCellData fv = dataGridCell.value;
        return ValueCellHolder(
            efv: fv.efv,
            feature: fv.feature,
            fv: fv.fv,
            afv: fv.afv,
            featuresDataSource: this); // adapt environments value columns
      }
    }).toList());
  }

  void updateDataGridSource() {
    notifyListeners();
  }
}

class CustomColumnSizer extends ColumnSizer {
  @override
  double computeCellHeight(GridColumn column, DataGridRow row,
      Object? cellValue, TextStyle textStyle) {
    double cellInfoIconsHeight = 40;
    double defaultCellHeight = 60;
    double height = 0;

    if (column.columnName == 'env') {
      row.getCells().forEach((cell) {
        if (cell.value is AggregatedFeatureCellData) {
          var typedCellData = cell.value as AggregatedFeatureCellData;
          if (typedCellData.fv != null) {
            var stratLength = typedCellData.fv!.rolloutStrategies.length;
            if (stratLength * defaultCellHeight > height) {
              height = stratLength * defaultCellHeight;
            }
          }
        }
      });
    }
    return height + defaultCellHeight + cellInfoIconsHeight;
  }
}
