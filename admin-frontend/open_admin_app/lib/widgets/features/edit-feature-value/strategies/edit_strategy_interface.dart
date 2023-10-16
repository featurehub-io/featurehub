import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';

abstract class EditStrategyBloc<T> {
  get feature => null;

  void addStrategy(EditingRolloutStrategy strategy) {}

  void updateStrategy() {}

  void removeStrategy(T strategy) {}

  Future validationCheck(RolloutStrategy strategy) async {}
}
