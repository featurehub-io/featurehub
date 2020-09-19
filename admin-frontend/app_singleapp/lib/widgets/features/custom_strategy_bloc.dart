import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

class CustomStrategyBloc extends Bloc {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final PerFeatureStateTrackingBloc fvBloc;
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

  void addStrategy(RolloutStrategy rs) {
    final strategies = _strategySource.value;
    strategies.add(rs);
    markDirty();
    _strategySource.add(strategies);
  }

  void updateStrategy() {
    final strategies = _strategySource.value;
    markDirty();
    _strategySource.add(strategies);
  }

  void removeStrategy(RolloutStrategy rs) {
    rs.id = 'removing';
    final strategies = _strategySource.value;
    strategies.removeWhere((e) => e.id == rs.id);
    markDirty();
    _strategySource.add(strategies);
  }

  bool validateStrategy(RolloutStrategy rs) {}

  @override
  void dispose() {}
}
