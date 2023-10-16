import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:multiselect/multiselect.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/features/feature-data-table/custom_column_sizer.dart';
import 'package:open_admin_app/widgets/features/feature-data-table/features_data_source.dart';
import 'package:open_admin_app/widgets/features/feature-data-table/handle_validation_messages.dart';
import 'package:open_admin_app/widgets/features/feature_dashboard_constants.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';
import 'package:syncfusion_flutter_core/theme.dart';
import 'package:syncfusion_flutter_datagrid/datagrid.dart';


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
  int _maxFeatures = 0;
  int _pageIndex = 0;
  int _rowsPerPage = 5;
  List<FeatureValueType> _selectedFeatureTypes = [];
  List<String> _selectedEnvironmentList = [];
  final CustomColumnSizer _customColumnSizer = CustomColumnSizer();

  @override
  void initState() {
    super.initState();
    final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);

    bloc.appFeatureValuesStream.listen((features) {
      if (mounted && features != null) {
          setState(() {
            _selectedEnvironmentList =
                bloc.selectedEnvironmentNamesByUser;
            var featuresList = FeatureStatusFeatures(features);
            _selectedFeatureTypes = bloc.selectedFeatureTypesByUser;
            _featuresDataSource = FeaturesDataSource(featuresList, widget.bloc,
                _searchTerm, _selectedFeatureTypes, _rowsPerPage);
            _maxFeatures = features.maxFeatures;
            _pageIndex = bloc.currentPageIndex-1;
          });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final debouncer = Debouncer(milliseconds: 500);
      List<GridColumn> gridColumnsList = [];
      return StreamBuilder<ApplicationFeatureValues?>(
          stream: widget.bloc.appFeatureValuesStream,
          builder: (context, snapshot) {
            if (snapshot.hasData &&
                snapshot.data!.environments.isEmpty) {
              return const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  NoEnvironmentMessage(),
                ],
              );
            }
            else if (snapshot.hasData) {
              gridColumnsList = snapshot.data!.environments
                  .map(
                    (entry) => GridColumn(
                      columnName: "env",
                      label: Container(
                          padding: const EdgeInsets.all(8.0),
                          alignment: Alignment.center,
                          child: Text(entry.environmentName,style: const TextStyle(fontWeight: FontWeight.bold))),
                      visible: _selectedEnvironmentList
                          .contains(entry.environmentName),
                    ),
                  )
                  .toList();

              var featuresList = FeatureStatusFeatures(snapshot.data!);
              _featuresDataSource = FeaturesDataSource(
                  featuresList,
                  widget.bloc,
                  _searchTerm,
                  _selectedFeatureTypes,
                  _rowsPerPage);
              _maxFeatures = snapshot.data!.maxFeatures;

              return Card(
                    elevation: 1,
                    color: Theme.of(context).colorScheme.surfaceVariant.withOpacity(0.5),
                    surfaceTintColor: Colors.transparent,
                    shadowColor: Colors.transparent,
                child: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.start,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Wrap(
                        spacing: 16.0,
                        runSpacing: 16.0,
                        children: [
                          Container(
                            constraints:
                                const BoxConstraints(maxWidth: 400, maxHeight: 40),
                            child: DropDownMultiSelect(
                                hint: Text("Select environments to display",
                                    style: Theme.of(context).textTheme.bodyMedium),
                                onChanged: (List<String> selectedValues) {
                                  setState(() {
                                    _selectedEnvironmentList = selectedValues;
                                  });
                                  widget.bloc.updateShownEnvironments(selectedValues);
                                },
                                icon: const Icon(
                                  Icons.visibility_sharp,
                                  size: 18,
                                ),
                                options: snapshot.data!.environments
                                    .map((e) => e.environmentName)
                                    .toList(),
                                selectedValues: _selectedEnvironmentList
                                // whenEmpty: 'Select Something',
                                ),
                          ),
                          Container(
                            constraints: const BoxConstraints(
                              maxWidth: 300,
                              maxHeight: 40,
                              minWidth: 30,
                            ),
                            child: DropDownMultiSelect(
                              icon: const Icon(
                                Icons.filter_alt,
                                size: 18,
                              ),
                              hint: Text("Filter by feature type",
                                  style: Theme.of(context).textTheme.bodyMedium),
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
                          Container(
                            constraints: const BoxConstraints(
                                maxWidth: 300, minWidth: 150, maxHeight: 40),
                            child: TextField(
                              decoration: InputDecoration(
                                hintText: 'Search features',
                                hintStyle: Theme.of(context).textTheme.bodyMedium,
                                suffixIcon: const Icon(Icons.search, size: 18),
                                border: const OutlineInputBorder(),
                              ),
                              onChanged: (val) {
                                debouncer.run(() {
                                  setState(() {
                                    _searchTerm = val;
                                    widget.bloc.getApplicationFeatureValuesData(
                                        widget.bloc.applicationId!,
                                        _searchTerm,
                                        _selectedFeatureTypes,
                                        _rowsPerPage,
                                        _pageIndex);
                                  });
                                });
                              },
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 24.0),
                      if(featuresList.applicationFeatureValues.features.isEmpty) const Text("No features to display"),
                      if(featuresList.applicationFeatureValues.features.isNotEmpty) SizedBox(
                        height: tableHeight,
                        child: SfDataGridTheme(
                          data: SfDataGridThemeData(headerColor: Theme.of(context).colorScheme.primaryContainer),
                          child: SfDataGrid(
                            source: _featuresDataSource,
                            gridLinesVisibility: GridLinesVisibility.both,
                            headerGridLinesVisibility: GridLinesVisibility.both,
                            isScrollbarAlwaysShown: true,
                            rowsPerPage: rowsPerPage,
                            defaultColumnWidth: featureValueCellWidth,
                            columnSizer: _customColumnSizer,
                            frozenColumnsCount: 1,
                            horizontalScrollPhysics: const ClampingScrollPhysics(),
                            verticalScrollPhysics: const ClampingScrollPhysics(),
                            onQueryRowHeight: (details) {
                              return details.getIntrinsicRowHeight(details.rowIndex);
                            },
                            columns: <GridColumn>[
                              GridColumn(
                                // minimumWidth: featureNameCellWidth,
                                // maximumWidth: featureNameCellWidth,
                                columnName: 'features name',
                                label: Container(
                                    padding: const EdgeInsets.all(16.0),
                                    alignment: Alignment.center,
                                    child: const Text(
                                      "Features", style: TextStyle(fontWeight: FontWeight.bold)
                                    )),
                              ),
                              ...gridColumnsList,
                            ],
                          ),
                        ),
                      ),
                      if (_maxFeatures >
                          rowsPerPage) // only display paginator if needed
                        SfDataPager(
                          delegate: _featuresDataSource,
                          pageCount:
                              (_maxFeatures / _rowsPerPage).ceil().ceilToDouble(),
                          direction: Axis.horizontal,
                          onRowsPerPageChanged: (rpp) {
                            _rowsPerPage = rpp ?? 5;
                            widget.bloc.getApplicationFeatureValuesData(
                                widget.bloc.applicationId!,
                                _searchTerm,
                                _selectedFeatureTypes,
                                _rowsPerPage,
                                _pageIndex);
                          },
                          availableRowsPerPage: const [5, 10, 15, 20],
                        )
                    ],
                  ),
                ),
              );
            } else {
              return const Center(
                child: CircularProgressIndicator(
                  strokeWidth: 3,
                ),
              );
            }
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
