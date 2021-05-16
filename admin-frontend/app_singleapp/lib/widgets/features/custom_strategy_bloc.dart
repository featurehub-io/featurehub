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
  final _rolloutStartegyAttributeList =
      BehaviorSubject<List<RolloutStrategyAttribute>>();
  Stream<List<RolloutStrategyAttribute>> get attributes =>
      _rolloutStartegyAttributeList.stream;

  Stream<List<RolloutStrategy>> get strategies => _strategySource.stream;

  CustomStrategyBloc(this.environmentFeatureValue, this.feature, this.fvBloc)
      : featureValue = fvBloc
            .featureValueByEnvironment(environmentFeatureValue.environmentId!) {
    _strategySource.add(featureValue.rolloutStrategies);
  }

  void markDirty() {
    fvBloc.dirty(environmentFeatureValue.environmentId!, (current) {
      current.customStrategies = _strategySource.value!;
    });
  }

  void addStrategy(RolloutStrategy rs) {
    final strategies = _strategySource.value!;
    strategies.add(rs);
    markDirty();
    _strategySource.add(strategies);
    // print('added strategy is $rs');
  }

  void updateStrategy() {
    final strategies = _strategySource.value!;
    markDirty();
    _strategySource.add(strategies);
  }

  void removeStrategy(RolloutStrategy rs) {
    rs.id = 'removing';
    final strategies = _strategySource.value!;
    strategies.removeWhere((e) => e.id == rs.id);
    markDirty();
    _strategySource.add(strategies);
  }

  void addStrategyAttribute() {
    // _strategySource.value
    var rsa = RolloutStrategyAttribute();
    final attributes = _strategySource.value!.last.attributes;
    attributes.add(rsa);
    _rolloutStartegyAttributeList.add(attributes);
  }

  void updateAttribute(attribute) {}

  @override
  void dispose() {}

  /// this goes through the strategies and ensures they have unique ids
  /// unique based on this specific feature value
  void ensureStrategiesAreUnique() {
    final strategies = _strategySource.value;

    if (strategies == null) {
      return;
    }

    var start = DateTime.now().microsecond;

    final strategiesById = <String, RolloutStrategy>{};

    strategies.forEach((s) {
      if (s.id != null) {
        strategiesById[s.id!] = s;
      }
    });

    strategies.forEach((s) {
      if (s.id == null) {
        while (strategiesById[start.toString()] != null) {
          start++;
        }
        s.id = start.toString();
        strategiesById[s.id!] = s;
      }
    });
  }

  Future<RolloutStrategyValidationResponse> validationCheck(
      RolloutStrategy strategy) async {
    // we need a list of strategies to send to the server, only 1 of which will be the created
    // one
    var strategies =
        _strategySource.value!.where((s) => s.id != strategy.id).toList();

    strategy.id ??= 'created';

    strategies.add(strategy);

    return fvBloc.featuresOnTabBloc.featureStatusBloc
        .validationCheck(strategies, <RolloutStrategyInstance>[]);
  }
}
