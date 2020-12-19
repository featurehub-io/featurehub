import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/feature_value_updated_by.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/json/json_strategy_card.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/lock_unlock_switch.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategies/split_rollout_button.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class JsonCellHolder extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final PerFeatureStateTrackingBloc fvBloc;

  const JsonCellHolder({Key key, this.environmentFeatureValue, this.fvBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final strategyBloc =
        fvBloc.matchingCustomStrategyBloc(environmentFeatureValue);
    return StreamBuilder<List<RolloutStrategy>>(
        stream: strategyBloc.strategies,
        builder: (context, snapshot) {
          return Column(
            mainAxisSize: MainAxisSize.max,
            children: [
              LockUnlockSwitch(
                environmentFeatureValue: environmentFeatureValue,
                fvBloc: fvBloc,
              ),
              JsonStrategyCard(
                strBloc: strategyBloc,
              ),
              if (snapshot.hasData)
                for (RolloutStrategy strategy in snapshot.data)
                  JsonStrategyCard(
                    strBloc: strategyBloc,
                    rolloutStrategy: strategy,
                  ),
              StreamBuilder<bool>(
                  stream: fvBloc.environmentIsLocked(
                      environmentFeatureValue.environmentId),
                  builder: (context, snapshot) {
                    if (snapshot.hasData) {
                      final canChangeValue = environmentFeatureValue.roles
                          .contains(RoleType.CHANGE_VALUE);
                      var editable = !snapshot.data && canChangeValue;
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
