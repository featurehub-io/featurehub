

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
    // convert the "editing rollout strategy" we have been editing back to a normal strategy
    // but with a null value
    var strategy = rs.toRolloutStrategy(null)!;

    // create a list of strategies, taking all the existing ones except for one where the id of
    // the one we were editing matches the one in the list (i.e. replace the one in the list with
    // this one)
    final customStrategies = [strategy, ...
        fvStrategyBloc.featureValue.rolloutStrategies!.where((rs) => rs.id != strategy.id)];

    // now go and do an evaluation
    return fvStrategyBloc.perApplicationFeaturesBloc.validationCheck(
        customStrategies,
        fvStrategyBloc.featureValue.rolloutStrategyInstances ?? []);
  }
}
