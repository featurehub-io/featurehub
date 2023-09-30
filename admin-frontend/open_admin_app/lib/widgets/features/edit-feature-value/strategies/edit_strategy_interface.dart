import 'package:mrapi/api.dart';

abstract class EditStrategyBloc<T> {
  get feature => null;

  void addStrategy(T strategy) {}

  void updateStrategy() {}

  void removeStrategy(T strategy) {}

  Future validationCheck(RolloutStrategy strategy) async {}
}
