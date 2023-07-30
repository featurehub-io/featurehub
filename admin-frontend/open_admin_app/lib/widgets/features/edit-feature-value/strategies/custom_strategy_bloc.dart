import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/edit_strategy_interface.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';
import 'package:open_admin_app/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:rxdart/rxdart.dart';

class CustomStrategyBloc extends Bloc
    implements EditStrategyBloc<RolloutStrategy> {
  final EnvironmentFeatureValues environmentFeatureValue;
  @override
  final Feature feature;
  final PerFeatureStateTrackingBloc fvBloc;
  final FeatureValue featureValue;
  final PerApplicationFeaturesBloc bloc;

  final _strategySource =
      BehaviorSubject<List<RolloutStrategy>>.seeded(<RolloutStrategy>[]);
  final _rolloutStrategyAttributeList =
      BehaviorSubject<List<RolloutStrategyAttribute>>();
  Stream<List<RolloutStrategyAttribute>> get attributes =>
      _rolloutStrategyAttributeList.stream;

  Stream<List<RolloutStrategy>> get strategies => _strategySource.stream;

  CustomStrategyBloc(this.environmentFeatureValue, this.feature, this.fvBloc,
      this.bloc, this.featureValue) {
    for (var rs in featureValue.rolloutStrategies) {
      rs.id ??= makeStrategyId(existing: featureValue.rolloutStrategies);
      if (rs.attributes.isNotEmpty == true) {
        for (var rsa in rs.attributes) {
          rsa.id ??= makeStrategyId();
        }
      }
    }

    _strategySource.add(featureValue.rolloutStrategies);
  }

  @override
  void addStrategy(RolloutStrategy rs) {
    rs.id ??= makeStrategyId(existing: _strategySource.value);
    List<RolloutStrategy> strategies = _strategySource.value;
    if (strategies.isNotEmpty) {
      strategies.add(rs);
      _strategySource.add(strategies);
    } else {
      _strategySource.add([rs]);
      strategies = [rs];
    }
    fvBloc.updateFeatureValueStrategies(strategies);
  }

  @override
  void updateStrategy() {
    final strategies = _strategySource.value;
    _strategySource.add(strategies);
  }

  void updateStrategyAndFeatureValue() {
    final strategies = _strategySource.value;
    _strategySource.add(strategies);
    fvBloc.updateFeatureValueStrategies(strategies);
  }

  @override
  void removeStrategy(RolloutStrategy rs) {
    // tag it to ensure it has a number so we can remove it
    rs.id ??= makeStrategyId();
    final strategies = _strategySource.value;
    strategies.removeWhere((e) => e.id == rs.id);
    _strategySource.add(strategies);
  }

  @override
  void addStrategyAttribute() {
    final rsa = RolloutStrategyAttribute();
    rsa.id ??= makeStrategyId(existing: _strategySource.value);
    final attributes = _strategySource.value.last.attributes;
    attributes.add(rsa);
    _rolloutStrategyAttributeList.add(attributes);
  }

  @override
  void updateAttribute(attribute) {}

  @override
  void dispose() {}

  /// this goes through the strategies and ensures they have unique ids
  /// unique based on this specific feature value
  void ensureStrategiesAreUnique() {
    final strategies = _strategySource.value;

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

  @override
  Future<RolloutStrategyValidationResponse> validationCheck(
      RolloutStrategy strategy) async {
    // we need a list of strategies to send to the server, only 1 of which will be the created
    // one
    var strategies =
        _strategySource.value.where((s) => s.id != strategy.id).toList();

    strategy.id ??= makeStrategyId(existing: strategies);

    strategies.add(strategy);

    return bloc.validationCheck(strategies, <RolloutStrategyInstance>[]);
  }

  @override
  uniqueStrategyId() {
    return makeStrategyId(existing: _strategySource.value);
  }
}
