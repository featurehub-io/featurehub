import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

import 'feature_values_bloc.dart';

class CustomStrategyBloc extends Bloc {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;
  final FeatureValue featureValue;

  final _strategySource =
  BehaviorSubject<List<RolloutStrategy>>.seeded(<RolloutStrategy>[]);

  Stream<List<RolloutStrategy>> get strategies => _strategySource.stream;

  CustomStrategyBloc(this.environmentFeatureValue, this.feature, this.fvBloc)
      : featureValue = fvBloc
      .featureValueByEnvironment(environmentFeatureValue.environmentId) {
    _strategySource.add(featureValue.rolloutStrategies);
  }

  void markDirty() {
    fvBloc.dirty(environmentFeatureValue.environmentId, (current) {
      current.customStrategies = _strategySource.value;
    });
  }

  // call from + Add Strategy to add one
  void addStrategy(RolloutStrategy rs) {
    final strategies = _strategySource.value;
    strategies.add(rs);
    markDirty();
    _strategySource.add(strategies);
  }

  void updateStrategy(RolloutStrategy rs) {
    final strategies = _strategySource.value;
    strategies.firstWhere((element) => element.id == rs.id)..name = rs.name..percentage = rs.percentage;
    markDirty();
    _strategySource.add(strategies);
  }

  @override
  void dispose() {}
}
