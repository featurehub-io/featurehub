import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/feature_value_updated_by.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/lock_unlock_switch.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/split_rollout_button.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/string/string_strategy_card.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class StringCellHolder extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final PerFeatureStateTrackingBloc fvBloc;

  const StringCellHolder(
      {Key key, this.environmentFeatureValue, this.feature, this.fvBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      creator: (_c, _b) =>
          CustomStrategyBloc(environmentFeatureValue, feature, fvBloc),
      child: Builder(builder: (ctx) {
        final strategyBloc = BlocProvider.of<CustomStrategyBloc>(ctx);
        return StreamBuilder<List<RolloutStrategy>>(
            stream: strategyBloc.strategies,
            builder: (context, snapshot) {
              return Column(
                mainAxisSize: MainAxisSize.max,
                children: [
                  LockUnlockSwitch(
                    environmentFeatureValue: environmentFeatureValue,
                    feature: feature,
                    fvBloc: fvBloc,
                  ),
                  StringStrategyCard(
                    strBloc: strategyBloc,
                  ),
                  if (snapshot.hasData)
                    for (RolloutStrategy strategy in snapshot.data)
                      StringStrategyCard(
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
      }),
    );
  }
}
