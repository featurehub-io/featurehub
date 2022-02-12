import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/feature_dashboard_constants.dart';
import 'package:open_admin_app/widgets/features/feature_value_status_tags.dart';
import 'package:open_admin_app/widgets/features/table-collapsed-view/value_cell.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/value_cell_holder.dart';
import 'package:open_admin_app/widgets/features/tabs_bloc.dart';

class FeatureValueCell extends StatelessWidget {
  final FeaturesOnThisTabTrackerBloc tabsBloc;
  final FeatureValue? value;
  final EnvironmentFeatureValues efv;
  final Feature feature;

  const FeatureValueCell(
      {Key? key,
      required this.tabsBloc,
      this.value,
      required this.efv,
      required this.feature})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<Set<String>?>(
        stream: tabsBloc.featureCurrentlyEditingStream,
        builder: (context, snapshot) {
          final amSelected =
              (snapshot.hasData && snapshot.data!.contains(feature.key));

          Widget cellWidget;
          if (!amSelected) {
            cellWidget = CollapsedViewValueCellHolder(
              fv: value,
              efv: efv,
              feature: feature,
            );

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
          } else if ((value == null || value!.id == null) &&
              efv.roles.isEmpty) {
            cellWidget = noAccessTag(null);
          } else {
            final fvBloc = tabsBloc.featureValueBlocs[feature.key]!;

            switch (feature.valueType!) {
              case FeatureValueType.BOOLEAN:
                cellWidget = ValueCellHolder(
                  environmentFeatureValue: efv,
                  fvBloc: fvBloc,
                  featureValueType: FeatureValueType.BOOLEAN,
                );
                break;
              case FeatureValueType.STRING:
                cellWidget = ValueCellHolder(
                    environmentFeatureValue: efv,
                    fvBloc: fvBloc,
                    featureValueType: FeatureValueType.STRING);
                break;
              case FeatureValueType.NUMBER:
                cellWidget = ValueCellHolder(
                    environmentFeatureValue: efv,
                    fvBloc: fvBloc,
                    featureValueType: FeatureValueType.NUMBER);
                break;
              case FeatureValueType.JSON:
                cellWidget = ValueCellHolder(
                    environmentFeatureValue: efv,
                    fvBloc: fvBloc,
                    featureValueType: FeatureValueType.JSON);
                break;
            }
          }

          final extra = tabsBloc.featureExtraCellHeight(feature);
          // final extra = 35;
          // final baseH = (amSelected ? selectedRowHeight : unselectedRowHeight);
          // final panelH = extra + baseH;
          // print("${feature.key} is $baseH + $extra = $panelH");

          return SizedBox(
              height: extra +
                  (amSelected
                      ? selectedRowHeight - 1
                      : unselectedRowHeight - 1),
              child: cellWidget);
        });
  }
}
