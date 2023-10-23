import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/fhos_logger.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:rxdart/rxdart.dart';

class EditingFeatureValueBloc implements Bloc {
  final Feature feature;
  final String applicationId;

  late FeatureServiceApi _featureServiceApi;

  final ApplicationFeatureValues applicationFeatureValues;
  final EnvironmentFeatureValues environmentFeatureValue;
  final PerApplicationFeaturesBloc _featureStatusBloc;
  late String environmentId;
  final FeatureValue featureValue;

  // actually ORIGINAL feature value (before changes)
  late FeatureValue currentFeatureValue;

  late final BehaviorSubject<List<RolloutStrategy>> _strategySource;
  final _rolloutStrategyAttributeList =
      BehaviorSubject<List<RolloutStrategyAttribute>>();
  Stream<List<RolloutStrategyAttribute>> get attributes =>
      _rolloutStrategyAttributeList.stream;

  Stream<List<RolloutStrategy>> get strategies => _strategySource.stream;

  EditingFeatureValueBloc(
      this.applicationId,
      this.feature,
      this.featureValue,
      this.environmentFeatureValue,
      PerApplicationFeaturesBloc featureStatusBloc,
      this.applicationFeatureValues)
      : _featureStatusBloc = featureStatusBloc {
    _featureServiceApi =
        FeatureServiceApi(featureStatusBloc.mrClient.apiClient);
    currentFeatureValue = FeatureValue.fromJson(featureValue
        .toJson()); // keeping original featureValue cached for resets
    _strategySource = BehaviorSubject<List<RolloutStrategy>>.seeded(
        [...currentFeatureValue.rolloutStrategies ?? []]);
    environmentId = environmentFeatureValue.environmentId;
    addFeatureValueToStream(featureValue);
  }

  /*
   * This takes the result of the adding of a new strategy and converts it back to a RolloutStrategy
   */
  void addStrategy(EditingRolloutStrategy rs) {
    List<RolloutStrategy> strategies = _strategySource.value;

    final index = strategies.indexWhere((s) => s.id == rs.id);
    if (index == -1) {
      dynamic value;

      if (feature.valueType == FeatureValueType.BOOLEAN) {
        value = !(featureValue.valueBoolean ?? false);
      }

      strategies.add(rs.toRolloutStrategy(value)!);
    } else {
      strategies[index] = rs.toRolloutStrategy(strategies[index].value)!;
    }

    updateFeatureValueStrategies(strategies);
  }

  void updateStrategy() {
    final strategies = _strategySource.value;
    _strategySource.add(strategies);
  }

  void updateStrategyAndFeatureValue() {
    final strategies = _strategySource.value;
    _strategySource.add(strategies);
    updateFeatureValueStrategies(strategies);
  }

  void removeStrategy(RolloutStrategy rs) {
    // tag it to ensure it has a number so we can remove it
    final strategies = _strategySource.value;
    fhosLogger.fine(
        "removing strategy ${rs.id} from list ${strategies.map((e) => e.id)}");
    strategies.removeWhere((e) => e.id == rs.id);
    _strategySource.add(strategies);
  }

  updateFeatureValueLockedStatus(bool locked) {
    currentFeatureValue.locked = locked;
    addFeatureValueToStream(currentFeatureValue);
  }

  void updateFeatureValueRetiredStatus(bool? retired) {
    currentFeatureValue.retired = retired ?? false;
    addFeatureValueToStream(currentFeatureValue);
  }

  void updateFeatureValueStrategies(List<RolloutStrategy> strategies) {
    currentFeatureValue.rolloutStrategies = strategies;
    addFeatureValueToStream(currentFeatureValue);
    _strategySource.add(strategies);
  }

  void updateFeatureValueDefault(replacementValue) {
    switch (feature.valueType) {
      case FeatureValueType.BOOLEAN:
        currentFeatureValue.valueBoolean = replacementValue;
        break;
      case FeatureValueType.STRING:
        currentFeatureValue.valueString = replacementValue;
        break;
      case FeatureValueType.NUMBER:
        currentFeatureValue.valueNumber = replacementValue;
        break;
      case FeatureValueType.JSON:
        currentFeatureValue.valueJson = replacementValue;
        break;
    }
    addFeatureValueToStream(currentFeatureValue);
  }

  final _currentFv = BehaviorSubject<FeatureValue>();

  get currentFv => _currentFv.stream;

  PerApplicationFeaturesBloc get perApplicationFeaturesBloc =>
      _featureStatusBloc;

  addFeatureValueToStream(FeatureValue fv) {
    _currentFv.add(fv);
  }

  @override
  void dispose() {
    _currentFv.close();
  }

  saveFeatureValueUpdates() async {
    currentFeatureValue.rolloutStrategies = _strategySource.value;
    await _featureServiceApi.updateAllFeatureValuesByApplicationForKey(
        applicationId, feature.key, [currentFeatureValue]);
    await _featureStatusBloc.updateApplicationFeatureValuesStream();
  }
}
