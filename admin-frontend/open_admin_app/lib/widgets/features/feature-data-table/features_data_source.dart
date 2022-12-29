import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/cell-view/value_cell.dart';
import 'package:open_admin_app/widgets/features/edit-feature/feature_cell_holder.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';
import 'package:syncfusion_flutter_datagrid/datagrid.dart';
import 'package:flutter/material.dart';


class FeaturesDataSource extends DataGridSource {
  /// Creates the data source class with required details.
  final FeatureStatusFeatures data;
  final PerApplicationFeaturesBloc bloc;
  final String searchTerm;
  final List<FeatureValueType> selectedFeatureTypes;
  final int rowsPerPage;

  FeaturesDataSource(
      this.data, this.bloc, this.searchTerm, this.selectedFeatureTypes, this.rowsPerPage) {
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
      bloc.getApplicationFeatureValuesData(bloc.applicationId!, searchTerm,
          selectedFeatureTypes, rowsPerPage, newPageIndex);
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
            return FeatureCellHolder(feature: feature); // adapt feature cells
          } else {
            AggregatedFeatureCellData fv = dataGridCell.value;
            return ValueCellHolder(
              efv: fv.efv,
              feature: fv.feature,
              fv: fv.fv,
              afv: fv.afv,
            ); // adapt feature value cells
          }
        }).toList());
  }

  void updateDataGridSource() {
    notifyListeners();
  }
}

class AggregatedFeatureCellData {
  final EnvironmentFeatureValues efv;
  final ApplicationFeatureValues afv;
  final Feature feature;
  final FeatureValue? fv;

  AggregatedFeatureCellData(
      {required this.afv, required this.efv, required this.feature, this.fv});
}
