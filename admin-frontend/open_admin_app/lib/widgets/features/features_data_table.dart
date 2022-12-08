import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';

import 'package:mrapi/api.dart';
import 'package:multiselect/multiselect.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/features/feature_cell_holder.dart';
import 'package:open_admin_app/widgets/features/features_overview_table_widget.dart';

import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';
import 'package:open_admin_app/widgets/features/table-collapsed-view/value_cell.dart';
import 'package:syncfusion_flutter_datagrid/datagrid.dart';

import 'feature_dashboard_constants.dart';

class FeaturesDataTable extends StatefulWidget {
  const FeaturesDataTable({Key? key, this.title, required this.bloc})
      : super(key: key);
  final String? title;
  final PerApplicationFeaturesBloc bloc;

  @override
  _FeaturesDataTableState createState() => _FeaturesDataTableState();
}

class _FeaturesDataTableState extends State<FeaturesDataTable> {
  late FeaturesDataSource _featuresDataSource;
  List<FeatureStatusFeatures> featuresList = [];

  String _searchTerm = '';
  bool _loading = true;
  int _maxFeatures = 0;
  int _pageIndex = 0;
  List<FeatureValueType> _selectedFeatureTypes = [];
  List<String> _selectedEnvironmentList = [];
  final CustomColumnSizer _customColumnSizer = CustomColumnSizer();

  @override
  void initState() {
    super.initState();
    final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);

    bloc.appFeatureValuesStream.listen((features) {
      if (mounted) {
        if (features != null) {
          setState(() {
            _selectedEnvironmentList =
                features.environments.map((e) => e.environmentName!).toList();
            var featuresList = FeatureStatusFeatures(features);
            _featuresDataSource = FeaturesDataSource(
                featuresList, widget.bloc, _searchTerm, _selectedFeatureTypes);
            _maxFeatures = features.maxFeatures;
            _loading = false;
          });
        } else {
          setState(() {
            _loading = true;
          });
        }
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final _debouncer = Debouncer(milliseconds: 500);
    if (!_loading) {
      List<GridColumn> gridColumnsList = [];
      return StreamBuilder<ApplicationFeatureValues?>(
          stream: widget.bloc.appFeatureValuesStream,
          builder: (context, snapshot) {
            if (snapshot.hasData && snapshot.data!.features.isNotEmpty) {
              print("rebuild");
              gridColumnsList = snapshot.data!.environments
                  .map(
                    (entry) => GridColumn(
                      columnName: "env",
                      label: Container(
                          padding: const EdgeInsets.all(8.0),
                          alignment: Alignment.center,
                          child: Text(entry.environmentName!)),
                      visible: _selectedEnvironmentList
                          .contains(entry.environmentName),
                    ),
                  )
                  .toList();

              var featuresList = FeatureStatusFeatures(snapshot.data!);
              _featuresDataSource = FeaturesDataSource(featuresList,
                  widget.bloc, _searchTerm, _selectedFeatureTypes);
              _maxFeatures = snapshot.data!.maxFeatures;

              return Column(
                mainAxisAlignment: MainAxisAlignment.start,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
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
                                widget.bloc.getApplicationFeatureValuesData(
                                    widget.bloc.applicationId!,
                                    _searchTerm,
                                    _selectedFeatureTypes,
                                    rowsPerPage,
                                    _pageIndex);
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
                        constraints:
                            const BoxConstraints(maxWidth: 600, maxHeight: 70),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text("Select environments to display",
                                style: Theme.of(context).textTheme.caption),
                            SizedBox(height: 8.0),
                            DropDownMultiSelect(
                                hint: Text("Select environments to display",
                                    style:
                                        Theme.of(context).textTheme.bodyMedium),
                                onChanged: (List<String> selectedValues) {
                                  setState(() {
                                    _selectedEnvironmentList = selectedValues;
                                  });
                                },
                                icon: const Icon(
                                  Icons.visibility_sharp,
                                  size: 18,
                                ),
                                options: snapshot.data!.environments
                                    .map((e) => e.environmentName!)
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
                          onChanged: (List<String> selection) {
                            setState(() {
                              _selectedFeatureTypes = selection
                                  .map((e) => convertToFeatureValueType(e))
                                  .toList();
                            });
                            widget.bloc.getApplicationFeatureValuesData(
                                widget.bloc.applicationId!,
                                _searchTerm,
                                _selectedFeatureTypes,
                                rowsPerPage,
                                _pageIndex);
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
                  SizedBox(
                    height: 600,
                    child: SfDataGrid(
                      source: _featuresDataSource,
                      isScrollbarAlwaysShown: true,
                      rowsPerPage: rowsPerPage,
                      defaultColumnWidth: 150,
                      columnSizer: _customColumnSizer,
                      frozenColumnsCount: 1,
                      onQueryRowHeight: (details) {
                        return details.getIntrinsicRowHeight(details.rowIndex);
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
                    ))
                ],
              );
            } else if (snapshot.hasData && snapshot.data!.features.isEmpty) {
              return const NoFeaturesMessage();
            } else {
              return const Center(
                child: CircularProgressIndicator(
                  strokeWidth: 3,
                ),
              );
            }
          });
    } else {
      return const Center(
        child: CircularProgressIndicator(
          strokeWidth: 3,
        ),
      );
    }
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
  final PerApplicationFeaturesBloc bloc;
  final String searchTerm;
  final List<FeatureValueType> selectedFeatureTypes;

  FeaturesDataSource(
      this.data, this.bloc, this.searchTerm, this.selectedFeatureTypes) {
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
                    return FeatureValue(
                        key: feature.key!,
                        locked: false,
                        environmentId: entry.value.environmentId); // workaround for feature values that are not set yet and are null
                  }))))
          .toList();
      return DataGridRow(cells: [
        DataGridCell<Feature>(columnName: 'feature name', value: feature),
        ..._cells,
      ]);
    }).toList();
  }

  @override
  Future<bool> handlePageChange(int oldPageIndex, int newPageIndex) async {
    if (oldPageIndex != newPageIndex) {
      // need to fix this function
      bloc.getApplicationFeatureValuesData(bloc.applicationId!, searchTerm,
          selectedFeatureTypes, rowsPerPage, newPageIndex);
      buildDataGridRows();
      updateDataGridSource();
      notifyDataSourceListeners();
      return Future<bool>.value(true);
    }
    return Future<bool>.value(false);
  }

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
        ); // adapt environments value columns
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
