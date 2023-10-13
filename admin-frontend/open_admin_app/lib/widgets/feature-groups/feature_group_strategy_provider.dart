


import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_group_bloc.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:open_admin_app/widgets/strategyeditor/strategy_editor_provider.dart';

class GroupRolloutStrategyProvider extends StrategyEditorProvider {
  final FeatureGroupBloc bloc;

  GroupRolloutStrategyProvider(this.bloc);

  @override
  Future<void> updateStrategy(EditingRolloutStrategy rs) async {
    bloc.addStrategy(rs);
  }

  @override
  Future<RolloutStrategyValidationResponse?> validateStrategy(EditingRolloutStrategy rs) {
    return bloc.validationCheck(rs.toRolloutStrategy(null)!);
  }
}
