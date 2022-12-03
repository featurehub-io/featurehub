import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:rxdart/rxdart.dart';
import 'package:uuid/uuid.dart';

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
    for (var rs in featureValue.rolloutStrategies) {
      rs.id ??= makeStrategyId(existing: featureValue.rolloutStrategies);
      if (rs.attributes.isNotEmpty == true) {
        for (var rsa in rs.attributes) {
          rsa.id = makeStrategyId();
        }
      }
    }

    _strategySource.add(featureValue.rolloutStrategies);
  }

  void markDirty() {
    fvBloc.dirty(environmentFeatureValue.environmentId!, (current) {
      current.customStrategies = _strategySource.value!;
    });
  }

  void addStrategy(RolloutStrategy rs) {
    rs.id ??= makeStrategyId(existing: _strategySource.value!);
    final strategies = _strategySource.value!;
    strategies.add(rs);
    markDirty();
    _strategySource.add(strategies);
  }

  void updateStrategy() {
    final strategies = _strategySource.value!;
    markDirty();
    _strategySource.add(strategies);
  }

  void removeStrategy(RolloutStrategy rs) {
    // tag it to ensure it has a number so we can remove it
    rs.id ??= makeStrategyId();
    final strategies = _strategySource.value!;
    strategies.removeWhere((e) => e.id == rs.id);
    markDirty();
    _strategySource.add(strategies);
  }

  void addStrategyAttribute() {
    // _strategySource.value
    final rsa = RolloutStrategyAttribute();
    rsa.id = makeStrategyId(existing: _strategySource.value!);
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

    final strategiesById = <String, RolloutStrategy>{};

    for (var s in strategies) {
      if (s.id != null) {
        strategiesById[s.id!] = s;
      }
    }

    for (var s in strategies) {
      if (s.id == null) {
        s.id = makeStrategyId(existing: strategies);
        strategiesById[s.id!] = s;
      }
    }
  }

  Future<RolloutStrategyValidationResponse> validationCheck(
      RolloutStrategy strategy) async {
    // we need a list of strategies to send to the server, only 1 of which will be the created
    // one
    var strategies =
        _strategySource.value!.where((s) => s.id != strategy.id).toList();

    strategy.id ??= makeStrategyId(existing: strategies);

    strategies.add(strategy);

    return fvBloc.featuresOnTabBloc.featureStatusBloc
        .validationCheck(strategies, <RolloutStrategyInstance>[]);
  }

  uniqueStrategyId() {
    return makeStrategyId(existing: _strategySource.value!);
  }
}
