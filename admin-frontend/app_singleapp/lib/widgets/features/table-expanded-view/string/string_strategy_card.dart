import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategy_card_widget.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/string/edit_string_value_container.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class StringStrategyCard extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final PerFeatureStateTrackingBloc fvBloc;
  final FeatureValue featureValue;
  final RolloutStrategy rolloutStrategy;
  final CustomStrategyBloc strBloc;

  StringStrategyCard(
      {Key key,
        @required this.environmentFeatureValue,
        @required this.feature,
        @required this.fvBloc,
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
            final enabled = editable && !snap.data;
            return StrategyCardWidget(
              editable: editable,
              strBloc: strBloc,
              rolloutStrategy: rolloutStrategy,
              fvBloc: fvBloc,
              editableHolderWidget: EditStringValueContainer(
                  canEdit: editable,
                  enabled: enabled,
                  rolloutStrategy: rolloutStrategy,
                  fvBloc: fvBloc,
                  strBloc: strBloc,
                  environmentFV: environmentFeatureValue,
                featureValue: featureValue,
                  ),
            );
          } else {
            return SizedBox.shrink();
          }
        });
  }
}
