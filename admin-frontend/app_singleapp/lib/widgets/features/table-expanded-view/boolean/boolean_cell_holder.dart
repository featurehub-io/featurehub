import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/boolean/boolean_strategy_card.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/feature_value_updated_by.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/lock_unlock_switch.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/split_rollout_button.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';


// represents the editing of the states of a single boolean flag on a single environment

class BooleanCellHolder extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final PerFeatureStateTrackingBloc fvBloc;

  const BooleanCellHolder(
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
              builder: (streamCtx, snap) {
                return Column(
                  mainAxisSize: MainAxisSize.max,
                  children: [
                    LockUnlockSwitch(
                      environmentFeatureValue: environmentFeatureValue,
                      feature: feature,
                      fvBloc: fvBloc,
                    ),
                    BooleanStrategyCard(
                      environmentFeatureValue: environmentFeatureValue,
                      feature: feature,
                      fvBloc: fvBloc,
                    ),
                    if (snap.hasData)
                      for (RolloutStrategy strategy in snap.data)
                        BooleanStrategyCard(
                          environmentFeatureValue: environmentFeatureValue,
                          feature: feature,
                          fvBloc: fvBloc,
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
                                bloc: strategyBloc,
                                fvBloc: fvBloc,
                                editable: editable);
                          } else {
                            return Container();
                          }
                        }),
                    FeatureValueUpdatedByCell(
                      environmentFeatureValue: environmentFeatureValue,
                      feature: feature,
                      fvBloc: fvBloc,
                    ),
                  ],
                );
              });
        }));
  }
}


