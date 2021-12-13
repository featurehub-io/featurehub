import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/feature_dashboard_constants.dart';
import 'package:open_admin_app/widgets/features/feature_value_status_tags.dart';
import 'package:open_admin_app/widgets/features/table-collapsed-view/value_cell.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/boolean/boolean_cell_holder.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/json/json_cell_holder.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/number/number_cell_holder.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/string/string_cell_holder.dart';
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
                cellWidget = BooleanCellHolder(
                    environmentFeatureValue: efv, fvBloc: fvBloc);
                break;
              case FeatureValueType.STRING:
                cellWidget = StringCellHolder(
                    environmentFeatureValue: efv, fvBloc: fvBloc);
                break;
              case FeatureValueType.NUMBER:
                cellWidget = NumberCellHolder(
                    environmentFeatureValue: efv, fvBloc: fvBloc);
                break;
              case FeatureValueType.JSON:
                cellWidget = JsonCellHolder(
                    environmentFeatureValue: efv, fvBloc: fvBloc);
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
                  (amSelected ? selectedRowHeight : unselectedRowHeight),
              child: cellWidget);
        });
  }
}
