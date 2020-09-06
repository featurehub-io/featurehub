import 'package:app_singleapp/widgets/features/dashboard-values-containers/non_boolean_value_cell.dart';
import 'package:app_singleapp/widgets/features/dashboard-values-containers/value_boolean_cell.dart';
import 'package:app_singleapp/widgets/features/feature_dashboard_constants.dart';
import 'package:app_singleapp/widgets/features/feature_value_row_boolean.dart';
import 'package:app_singleapp/widgets/features/feature_value_row_json.dart';
import 'package:app_singleapp/widgets/features/feature_value_row_number.dart';
import 'package:app_singleapp/widgets/features/feature_value_row_string.dart';
import 'package:app_singleapp/widgets/features/tabs_bloc.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';

class FeatureValueCell extends StatelessWidget {
  final TabsBloc tabsBloc;
  final FeatureValue value;
  final EnvironmentFeatureValues efv;
  final Feature feature;

  const FeatureValueCell(
      {Key key,
      @required this.tabsBloc,
      @required this.value,
      @required this.efv,
      this.feature})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<Set<String>>(
        stream: tabsBloc.featureCurrentlyEditingStream,
        builder: (context, snapshot) {
          final amSelected =
              (snapshot.hasData && snapshot.data.contains(feature.key));

          Widget cellWidget;
          if (!amSelected) {
            if (feature.valueType == FeatureValueType.BOOLEAN) {
              cellWidget = BooleanCell(
                fv: value,
                efv: efv,
                feature: feature,
              );
            } else {
              cellWidget = ValueCell(
                fv: value,
                efv: efv,
                feature: feature,
              );
            }

            cellWidget = InkWell(
                canRequestFocus: false,
                hoverColor: Colors.transparent,
                splashColor: Colors.transparent,
                highlightColor: Colors.transparent,
                mouseCursor: SystemMouseCursors.click,
//                behavior: HitTestBehavior.opaque,
                onTap: () => tabsBloc.hideOrShowFeature(feature),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    cellWidget,
                  ],
                ));
          } else if (feature == null) {
            cellWidget = Text('');
          } else {
            final fvBloc = tabsBloc.featureValueBlocs[feature.key];
            switch (feature.valueType) {
              case FeatureValueType.BOOLEAN:
                cellWidget = FeatureValueBooleanCellEditor(
                    environmentFeatureValue: efv,
                    feature: feature,
                    fvBloc: fvBloc);
                break;
              case FeatureValueType.STRING:
                cellWidget = FeatureValueStringCellEditor(
                    environmentFeatureValue: efv,
                    feature: feature,
                    fvBloc: fvBloc);
                break;
              case FeatureValueType.NUMBER:
                cellWidget = FeatureValueNumberCellEditor(
                    environmentFeatureValue: efv,
                    feature: feature,
                    fvBloc: fvBloc);
                break;
              case FeatureValueType.JSON:
                cellWidget = FeatureValueJsonCellEditor(
                    environmentFeatureValue: efv,
                    feature: feature,
                    fvBloc: fvBloc);
                break;
            }
          }

          final extra = tabsBloc.featureExtraCellHeight(feature);
          // final panelH =
          //     extra + (amSelected ? selectedRowHeight : unselectedRowHeight);

          return Container(
              height: extra +
                  (amSelected ? selectedRowHeight : unselectedRowHeight),
              child: cellWidget);
        });
  }
}
