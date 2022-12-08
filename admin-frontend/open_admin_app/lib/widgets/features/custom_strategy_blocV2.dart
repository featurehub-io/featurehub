import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';
import 'package:open_admin_app/widgets/features/per_feature_state_tracking_blocv2.dart';
import 'package:rxdart/rxdart.dart';
import 'package:uuid/uuid.dart';

const _strategyBlocUUidGenerator = Uuid();

class CustomStrategyBlocV2 extends Bloc {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final PerFeatureStateTrackingBlocV2 fvBloc;
  final FeatureValue featureValue;
  final PerApplicationFeaturesBloc bloc;

  final _strategySource =
  BehaviorSubject<List<RolloutStrategy>>.seeded(<RolloutStrategy>[]);
  final _rolloutStartegyAttributeList =
  BehaviorSubject<List<RolloutStrategyAttribute>>();
  Stream<List<RolloutStrategyAttribute>> get attributes =>
      _rolloutStartegyAttributeList.stream;

  Stream<List<RolloutStrategy>> get strategies => _strategySource.stream;

  CustomStrategyBlocV2(this.environmentFeatureValue, this.feature, this.fvBloc, this.bloc, this.featureValue)
      {
    for (var rs in featureValue.rolloutStrategies) {
      if (rs.id != null && rs.id!.length < 30) {
        rs.id = _strategyBlocUUidGenerator.v4();
      }
      if (rs.attributes.isNotEmpty == true) {
        for (var rsa in rs.attributes) {
          rsa.id = _strategyBlocUUidGenerator.v4();
        }
      }
    }

    _strategySource.add(featureValue.rolloutStrategies);
  }

  // void markDirty() {
  //   fvBloc.dirty(environmentFeatureValue.environmentId!, (current) {
  //     current.customStrategies = _strategySource.value!;
  //   });
  // }


  void addStrategy(RolloutStrategy rs) {
    rs.id ??= _strategyBlocUUidGenerator.v4();
    final strategies = _strategySource.value;
    strategies.add(rs);
    _strategySource.add(strategies);
  }

  void updateStrategy() {
    final strategies = _strategySource.value;
    _strategySource.add(strategies);
  }

  void removeStrategy(RolloutStrategy rs) {
    // tag it to ensure it has a number so we can remove it
    rs.id = _strategyBlocUUidGenerator.v4();
    final strategies = _strategySource.value;
    strategies.removeWhere((e) => e.id == rs.id);
    // markDirty();
    _strategySource.add(strategies);
  }

  void addStrategyAttribute() {
    // _strategySource.value
    final rsa = RolloutStrategyAttribute();
    rsa.id = _strategyBlocUUidGenerator.v4();
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
        s.id = _strategyBlocUUidGenerator.v4();
        strategiesById[s.id!] = s;
      }
    }
  }

  Future<RolloutStrategyValidationResponse> validationCheck(
      RolloutStrategy strategy) async {
    // we need a list of strategies to send to the server, only 1 of which will be the created
    // one
    var strategies =
    _strategySource.value.where((s) => s.id != strategy.id).toList();

    strategy.id ??= _strategyBlocUUidGenerator.v4();

    strategies.add(strategy);

    return bloc.validationCheck(strategies, <RolloutStrategyInstance>[]
    );
  }
}
