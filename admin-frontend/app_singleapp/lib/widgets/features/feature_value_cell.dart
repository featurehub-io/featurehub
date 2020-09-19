import 'package:app_singleapp/widgets/features/feature_dashboard_constants.dart';
import 'package:app_singleapp/widgets/features/feature_value_status_tags.dart';
import 'package:app_singleapp/widgets/features/table-collapsed-view/value_cell.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/boolean/boolean_cell_holder.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/json/json_cell_holder.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/number/number_cell_holder.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/string/string_cell_holder.dart';
import 'package:app_singleapp/widgets/features/tabs_bloc.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';

class FeatureValueCell extends StatelessWidget {
  final FeaturesOnThisTabTrackerBloc tabsBloc;
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
          } else if (feature == null) {
            cellWidget = Text('');
          } else if
          ((value == null || value.id == null) && efv.roles.isEmpty) {
          cellWidget = noAccessTag(null);

          }
          else {
            final fvBloc = tabsBloc.featureValueBlocs[feature.key];
            switch (feature.valueType) {
              case FeatureValueType.BOOLEAN:
                cellWidget = BooleanCellHolder(
                    environmentFeatureValue: efv,
                    feature: feature,
                    fvBloc: fvBloc);
                break;
              case FeatureValueType.STRING:
                cellWidget = StringCellHolder(
                    environmentFeatureValue: efv,
                    feature: feature,
                    fvBloc: fvBloc);
                break;
              case FeatureValueType.NUMBER:
                cellWidget = NumberCellHolder(
                    environmentFeatureValue: efv,
                    feature: feature,
                    fvBloc: fvBloc);
                break;
              case FeatureValueType.JSON:
                cellWidget = JsonCellHolder(
                    environmentFeatureValue: efv,
                    feature: feature,
                    fvBloc: fvBloc);
                break;
            }
          }

          final extra = tabsBloc.featureExtraCellHeight(feature);
          // final extra = 35;
          // final panelH =
          //     extra + (amSelected ? selectedRowHeight : unselectedRowHeight);
          // print("panelH is $panelH");

          return Container(
              height: extra +
                  (amSelected ? selectedRowHeight : unselectedRowHeight),
              child: cellWidget);
        });
  }
}
