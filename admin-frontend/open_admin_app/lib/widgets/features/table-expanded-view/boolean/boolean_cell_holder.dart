import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/boolean/boolean_strategy_card.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/feature_value_updated_by.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/lock_unlock_switch.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/strategies/split_rollout_button.dart';

// represents the editing of the states of a single boolean flag on a single environment

class BooleanCellHolder extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final PerFeatureStateTrackingBloc fvBloc;

  const BooleanCellHolder(
      {Key? key, required this.environmentFeatureValue, required this.fvBloc})
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
              BooleanStrategyCard(
                strBloc: strategyBloc,
              ),
              if (snap.hasData)
                for (RolloutStrategy strategy in snap.data!)
                  BooleanStrategyCard(
                    strBloc: strategyBloc,
                    rolloutStrategy: strategy,
                  ),
              StreamBuilder<bool>(
                  stream: fvBloc.environmentIsLocked(
                      environmentFeatureValue.environmentId!),
                  builder: (context, snapshot) {
                    if (snapshot.hasData) {
                      final canChangeValue = environmentFeatureValue.roles
                          .contains(RoleType.CHANGE_VALUE);
                      var editable = !snapshot.data! && canChangeValue;
                      return AddStrategyButton(
                          bloc: strategyBloc, editable: editable);
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
