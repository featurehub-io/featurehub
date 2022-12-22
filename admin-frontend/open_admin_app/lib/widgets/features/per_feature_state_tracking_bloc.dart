import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/custom_strategy_bloc.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';
import 'package:rxdart/rxdart.dart';



class PerFeatureStateTrackingBloc implements Bloc {
  final Feature feature;
  final String applicationId;

  late FeatureServiceApi _featureServiceApi;

  final _newFeatureValues = <String, FeatureValue>{};
  final _fvUpdates = <String, FeatureValue>{};
  final ApplicationFeatureValues applicationFeatureValues;
  final PerApplicationFeaturesBloc _featureStatusBloc;

  final _customStrategyBlocs =
      <EnvironmentFeatureValues, CustomStrategyBloc>{};

  FeatureValue? currentFeatureValue;

  PerFeatureStateTrackingBloc(
      this.applicationId,
      this.feature,
      FeatureValue featureValue,
      PerApplicationFeaturesBloc featureStatusBloc,
      this.applicationFeatureValues)
      : _featureStatusBloc = featureStatusBloc {
    _featureServiceApi = FeatureServiceApi(featureStatusBloc.mrClient.apiClient);
    currentFeatureValue = FeatureValue.fromJson(featureValue.toJson()); // keeping original featureValue cached for resets
    addFeatureValueToStream(featureValue);
  }

  updateFeatureValueLockedStatus(bool locked) {
    currentFeatureValue!.locked = locked;
    addFeatureValueToStream(currentFeatureValue!);
  }

  void updateFeatureValueRetiredStatus(bool? retired) {
    currentFeatureValue!.retired = retired;
    addFeatureValueToStream(currentFeatureValue!);
  }

  void updateFeatureValueStrategies(List<RolloutStrategy> strategies) {
    currentFeatureValue!.rolloutStrategies = strategies;
    addFeatureValueToStream(currentFeatureValue!);
  }

  void updateFeatureValueDefault(replacementValue) {
    switch (feature.valueType!) {
      case FeatureValueType.BOOLEAN:
        currentFeatureValue!.valueBoolean = replacementValue;
        break;
      case FeatureValueType.STRING:
        currentFeatureValue!.valueString = replacementValue;
        break;
      case FeatureValueType.NUMBER:
        currentFeatureValue!.valueNumber = replacementValue;
        break;
      case FeatureValueType.JSON:
        currentFeatureValue!.valueJson = replacementValue;
        break;
    }
    addFeatureValueToStream(currentFeatureValue!);
  }

  CustomStrategyBloc matchingCustomStrategyBloc(
      EnvironmentFeatureValues efv) {
    return _customStrategyBlocs.putIfAbsent(
        efv,
        () => CustomStrategyBloc(
            efv, feature, this, _featureStatusBloc, currentFeatureValue!));
  }

  final _currentFv = BehaviorSubject<FeatureValue>();

  get currentFv => _currentFv.stream;

  PerApplicationFeaturesBloc get perApplicationFeaturesBloc =>
      _featureStatusBloc;

  addFeatureValueToStream(FeatureValue fv) {
    _currentFv.add(fv);
  }

  // provides back a stream of updates to any listener for this cell
  FeatureValue featureValueByEnvironment(String envId) {
    return _fvUpdates.putIfAbsent(envId, () {
      final fv = _newFeatureValues.putIfAbsent(
          envId,
          () => FeatureValue(
                environmentId: envId,
                locked: false,
                key: feature.key!,
              ));

      return fv;
    });
  }

  @override
  void dispose() {
    for (var b in _customStrategyBlocs.values) {
      b.dispose();
    }
    _currentFv.close();
  }

  saveFeatureValueUpdates() async {
    await _featureServiceApi.updateAllFeatureValuesByApplicationForKey(
        applicationId, feature.key!, [currentFeatureValue!]);
    await _featureStatusBloc.updateApplicationFeatureValuesStream();

  }
}
