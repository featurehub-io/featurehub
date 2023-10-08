

import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:open_admin_app/widgets/strategyeditor/strategy_editor_provider.dart';

class FeatureValueStrategyProvider extends StrategyEditorProvider {
  final EditingFeatureValueBloc fvStrategyBloc;

  FeatureValueStrategyProvider(this.fvStrategyBloc);

  @override
  Future<void> updateStrategy(EditingRolloutStrategy rs) async {
    fvStrategyBloc.addStrategy(rs);
  }

  @override
  Future<RolloutStrategyValidationResponse?> validateStrategy(EditingRolloutStrategy rs) {
    var strategy = rs.toRolloutStrategy()!;

    final customStrategies = [strategy, ...
        fvStrategyBloc.featureValue.rolloutStrategies!.where((rs) => rs.id != strategy)];

    return fvStrategyBloc.perApplicationFeaturesBloc.validationCheck(
        customStrategies,
        fvStrategyBloc.featureValue.rolloutStrategyInstances ?? []);
  }
}
