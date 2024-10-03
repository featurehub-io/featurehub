import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/application-strategies/edit_application_strategy_bloc.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:open_admin_app/widgets/strategyeditor/strategy_editor_provider.dart';

class EditApplicationStrategyProvider extends StrategyEditorProvider {
  final EditApplicationStrategyBloc bloc;

  EditApplicationStrategyProvider(this.bloc);

  @override
  Future<void> updateStrategy(EditingRolloutStrategy rs) async {
    await bloc.addStrategy(rs);
  }

  @override
  Future<RolloutStrategyValidationResponse> validateStrategy(
      EditingRolloutStrategy rs) {
    return bloc.validationCheck(rs.toRolloutStrategy(null)!);
  }
}
