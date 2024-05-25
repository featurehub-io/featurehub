import 'dart:async';

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
  late FeatureHistoryServiceApi _featureHistoryServiceApi;

  final ApplicationFeatureValues applicationFeatureValues;
  final EnvironmentFeatureValues environmentFeatureValue;
  final PerApplicationFeaturesBloc _featureStatusBloc;
  late String environmentId;
  final FeatureValue featureValue;

  // actually ORIGINAL feature value (before changes)
  late FeatureValue currentFeatureValue;

  late StreamSubscription<FeatureValue> _featureValueStreamSubscription;

  late final BehaviorSubject<List<RolloutStrategy>> _strategySource;
  Stream<List<RolloutStrategy>> get strategies => _strategySource.stream;

  final _isFeatureValueUpdatedSource = BehaviorSubject<bool>.seeded(false);
  BehaviorSubject<bool> get isFeatureValueUpdatedStream =>
      _isFeatureValueUpdatedSource;

  final _currentFv = BehaviorSubject<FeatureValue>();
  get currentFv => _currentFv.stream;

  final _featureHistoryListSource = BehaviorSubject<FeatureHistoryItem?>();
  get featureHistoryListSource => _featureHistoryListSource.stream;

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
    _featureHistoryServiceApi =
        FeatureHistoryServiceApi(featureStatusBloc.mrClient.apiClient);
    currentFeatureValue = FeatureValue.fromJson(featureValue
        .toJson()); // keeping original featureValue cached for resets
    _strategySource = BehaviorSubject<List<RolloutStrategy>>.seeded(
        [...currentFeatureValue.rolloutStrategies ?? []]);
    environmentId = environmentFeatureValue.environmentId;
    addFeatureValueToStream(featureValue);
    _featureValueStreamSubscription = _currentFv.listen(featureValueHasChanged);
  }

  /*
   * This takes the result of the adding of a new strategy and converts it back to a RolloutStrategy
   */
  void addStrategy(EditingRolloutStrategy rs) {
    var strategies = _strategySource.value;

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

    currentFeatureValue.rolloutStrategies = strategies;
    addFeatureValueToStream(currentFeatureValue);
    _strategySource.add(strategies);
  }

  void updateStrategyValue() {
    final strategies = _strategySource.value;
    _strategySource.add(strategies);
    currentFeatureValue.rolloutStrategies = strategies;
    addFeatureValueToStream(currentFeatureValue);
  }

  void featureValueHasChanged(FeatureValue updatedFeatureValue) {
    if (featureValue != updatedFeatureValue) {
      _isFeatureValueUpdatedSource.add(true);
    } else {
      _isFeatureValueUpdatedSource.add(false);
    }
  }

  void removeStrategy(RolloutStrategy rs) {
    // tag it to ensure it has a number so we can remove it
    final strategies = _strategySource.value;
    fhosLogger.fine(
        "removing strategy ${rs.id} from list ${strategies.map((e) => e.id)}");
    strategies.removeWhere((e) => e.id == rs.id);
    updateStrategyValue();
  }

  updateFeatureValueLockedStatus(bool locked) {
    currentFeatureValue.locked = locked;
    addFeatureValueToStream(currentFeatureValue);
  }

  void updateFeatureValueRetiredStatus(bool? retired) {
    currentFeatureValue.retired = retired ?? false;
    addFeatureValueToStream(currentFeatureValue);
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

  PerApplicationFeaturesBloc get perApplicationFeaturesBloc =>
      _featureStatusBloc;

  addFeatureValueToStream(FeatureValue fv) {
    _currentFv.add(fv);
  }

  @override
  void dispose() {
    _featureValueStreamSubscription.cancel();
    _currentFv.close();
    _isFeatureValueUpdatedSource.close();
    _strategySource.close();
    _featureHistoryListSource.close();
  }

  saveFeatureValueUpdates() async {
    await _featureServiceApi.updateAllFeatureValuesByApplicationForKey(
        applicationId, feature.key, [currentFeatureValue]);
    await _featureStatusBloc.updateApplicationFeatureValuesStream();
  }

  getHistory() async {
    var featureHistory = await _featureHistoryServiceApi.listFeatureHistory(
      applicationId,
      order: FeatureHistoryOrder.desc,
    );
    var filtered = featureHistory.items
        .where((value) =>
            value.envId == environmentId && value.featureId == feature.id)
        .toList()[0];
    _featureHistoryListSource.add(filtered);
  }

  void clearHistory() {
    _featureHistoryListSource.add(null);
  }
}
