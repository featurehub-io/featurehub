import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/feature_value_updated_by.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/lock_unlock_switch.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/retire_feature_value_checkbox_widget.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/strategies/split_rollout_button.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/strategies/strategy_card.dart';

// represents the editing of the states of a single boolean flag on a single environment

class ValueCellHolder extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final PerFeatureStateTrackingBloc fvBloc;
  final FeatureValueType featureValueType;

  const ValueCellHolder(
      {Key? key,
      required this.environmentFeatureValue,
      required this.fvBloc,
      required this.featureValueType})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final strategyBloc =
        fvBloc.matchingCustomStrategyBloc(environmentFeatureValue);

    return StreamBuilder<List<RolloutStrategy>>(
        stream: strategyBloc.strategies,
        builder: (streamCtx, snap) {
          return Column(
            mainAxisSize: MainAxisSize.max,
            children: [
              LockUnlockSwitch(
                environmentFeatureValue: environmentFeatureValue,
                fvBloc: fvBloc,
              ),
              StreamBuilder<List<RolloutStrategy>>(
                  stream: strategyBloc.strategies,
                  builder: (context, snapshot) {
                    return Column(
                      mainAxisSize: MainAxisSize.max,
                      children: [
                        StrategyCard(
                            strBloc: strategyBloc,
                            featureValueType: featureValueType),
                        if (snapshot.hasData)
                          for (RolloutStrategy strategy in snapshot.data!)
                            StrategyCard(
                                strBloc: strategyBloc,
                                rolloutStrategy: strategy,
                                featureValueType: featureValueType),
                      ],
                    );
                  }),
              StreamBuilder<bool>(
                  stream: fvBloc.environmentIsLocked(
                      environmentFeatureValue.environmentId!),
                  builder: (context, snapshot) {
                    if (snapshot.hasData) {
                      final canChangeValue = environmentFeatureValue.roles
                          .contains(RoleType.CHANGE_VALUE);
                      var editable = !snapshot.data! && canChangeValue;
                      return Column(
                        children: [
                          const SizedBox(height: 8.0),
                          AddStrategyButton(
                              bloc: strategyBloc, editable: editable),
                          RetireFeatureValueCheckboxWidget(
                              environmentFeatureValue: environmentFeatureValue,
                              fvBloc: fvBloc,
                              editable: editable,
                              retired: fvBloc.isRetired(
                                  environmentFeatureValue.environmentId!)),
                          //this is where we need to pass retired from the actual value
                        ],
                      );
                    } else {
                      return Container();
                    }
                  }),
              FeatureValueUpdatedByCell(
                strBloc: strategyBloc,
              ),
            ],
          );
        });
  }
}
