import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/edit_boolean_value_dropdown_widget.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategy_cards_widget.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class FeatureValueBooleanEnvironmentCell extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final PerFeatureStateTrackingBloc fvBloc;
  final FeatureValue featureValue;
  final RolloutStrategy rolloutStrategy;
  final CustomStrategyBloc strBloc;

  FeatureValueBooleanEnvironmentCell(
      {Key key,
        this.environmentFeatureValue,
        this.feature,
        this.fvBloc,
        this.rolloutStrategy,
        this.strBloc})
      : featureValue = fvBloc
      .featureValueByEnvironment(environmentFeatureValue.environmentId),
        super(key: key);


  @override
  Widget build(BuildContext context) {
    return StreamBuilder<bool>(
        stream: fvBloc
            .environmentIsLocked(environmentFeatureValue.environmentId),
        builder: (ctx, snap) {
          if (snap.hasData) {
            final canChangeValue = environmentFeatureValue.roles
                .contains(RoleType.CHANGE_VALUE);
            var editable = !snap.data && canChangeValue;
            return StrategyCardsListWidget(
              editable: editable,
              strBloc: strBloc,
              rolloutStrategy: rolloutStrategy,
              fvBloc: fvBloc,
              editableHolderWidget: EditBooleanValueDropDownWidget(
                  editable: editable,
                  rolloutStrategy: rolloutStrategy,
                  fvBloc: fvBloc,
                  strBloc: strBloc,
                  environmentFV: environmentFeatureValue,
                  featureValue: featureValue),
            );
          } else {
            return SizedBox.shrink();
          }
        });
  }
}
