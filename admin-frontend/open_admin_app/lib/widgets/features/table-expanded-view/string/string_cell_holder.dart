import 'package:open_admin_app/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/string/string_strategy_card.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class StringCellHolder extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final PerFeatureStateTrackingBloc fvBloc;

  const StringCellHolder(
      {Key? key, required this.environmentFeatureValue, required this.fvBloc})
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
              StringStrategyCard(
                strBloc: strategyBloc,
              ),
              if (snapshot.hasData)
                for (RolloutStrategy strategy in snapshot.data!)
                  StringStrategyCard(
                    strBloc: strategyBloc,
                    rolloutStrategy: strategy,
                  ),
            ],
          );
        });
  }
}
