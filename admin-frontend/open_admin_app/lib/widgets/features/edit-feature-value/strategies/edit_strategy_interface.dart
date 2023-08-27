abstract class EditStrategyBloc<T> {
  get feature => null;

  void addStrategy(T strategy) {}

  void updateStrategy() {}

  void removeStrategy(T strategy) {}

  void addStrategyAttribute() {}

  /// this goes through the strategies and ensures they have unique ids
  /// unique based on this specific feature value
  void ensureStrategiesAreUnique() {}

  Future validationCheck(T strategy) async {}

  uniqueStrategyId() {}
}
