import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/features/feature-data-table/features_data_source.dart';
import 'package:open_admin_app/widgets/features/feature_dashboard_constants.dart';
import 'package:syncfusion_flutter_datagrid/datagrid.dart';

class CustomColumnSizer extends ColumnSizer {
  @override
  double computeCellHeight(GridColumn column, DataGridRow row,
      Object? cellValue, TextStyle textStyle) {
    double cellInfoIconsHeight = 40;
    double defaultCellHeight = strategyCardHeight + strategyCardPadding;
    double height = 0;

    if (column.columnName == 'env') {
      row.getCells().forEach((cell) {
        if (cell.value is AggregatedFeatureCellData) {
          var typedCellData = cell.value as AggregatedFeatureCellData;
          if (typedCellData.fv != null) {
            int stratLength = 0;
            stratLength = typedCellData.fv!.rolloutStrategies!.length +
                typedCellData.fv!.featureGroupStrategies!.length +
                typedCellData.fv!.rolloutStrategyInstances!.length;
            if (stratLength * defaultCellHeight > height) {
              height = stratLength * defaultCellHeight;
            }
          }
        }
      });
    }
    return height +
        defaultCellHeight +
        cellInfoIconsHeight +
        featureNameBoxExtraHeightForFeatureType;
  }
}
