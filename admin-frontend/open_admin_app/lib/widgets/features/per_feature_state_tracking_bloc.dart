import 'dart:async';
import 'dart:math';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/features/custom_strategy_bloc.dart';import 'package:rxdart/rxdart.dart';

import 'per_application_features_bloc.dart';

typedef DirtyCallback = bool Function(FeatureValue original);
typedef DirtyFeatureHolderCallback = void Function(
    FeatureValueDirtyHolder current);

///
/// this allows us to keep track of the things that can change
/// which include the value, the custom strategies, their order or values
/// and any linked shared strategies
///
class FeatureValueDirtyHolder {
  dynamic value;
  List<RolloutStrategy> customStrategies = [];
  List<RolloutStrategyInstance> sharedStrategies = [];
}

class PerFeatureStateTrackingBloc implements Bloc {
  final Feature feature;
  final String applicationId;

  // final String applicationId;
  final ManagementRepositoryClientBloc mrClient;
  late EnvironmentServiceApi _environmentServiceApi;
  late FeatureServiceApi _featureServiceApi;

  final _newFeatureValues = <String, FeatureValue>{};
  final _originalFeatureValues = <String, FeatureValue>{};
  final _fvUpdates = <String, FeatureValue>{};
  final ApplicationFeatureValues applicationFeatureValues;
  final PerApplicationFeaturesBloc _featureStatusBloc;

  final _customStrategyBlocs =
      <EnvironmentFeatureValues, CustomStrategyBloc>{};

  FeatureValue? currentFeatureValue;

  PerFeatureStateTrackingBloc(
      this.applicationId,
      this.feature,
      this.mrClient,
      FeatureValue featureValue,
      PerApplicationFeaturesBloc featureStatusBloc,
      this.applicationFeatureValues)
      : _featureStatusBloc = featureStatusBloc {
    _environmentServiceApi = EnvironmentServiceApi(mrClient.apiClient);
    _featureServiceApi = FeatureServiceApi(mrClient.apiClient);
    currentFeatureValue = featureValue;
    print("Current FV" + currentFeatureValue.toString());
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

  void updateFeatureValueDefault(replacementValue) {
    switch (feature.valueType!) {
      case FeatureValueType.BOOLEAN:
        currentFeatureValue!.valueBoolean = replacementValue;
        break;
      case FeatureValueType.STRING:
        currentFeatureValue!.valueString = replacementValue;
        break;
      case FeatureValueType.NUMBER:
        print(currentFeatureValue);
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
  }

  bool hasValue(FeatureEnvironment fe) {
    return _newFeatureValues[fe.environment!.id]?.valueBoolean != null ||
        _newFeatureValues[fe.environment!.id]?.valueString != null ||
        _newFeatureValues[fe.environment!.id]?.valueJson != null ||
        _newFeatureValues[fe.environment!.id]?.valueNumber != null;
  }

  void resetValue(FeatureEnvironment fe) {
    _newFeatureValues[fe.environment!.id]?.valueBoolean = null;
    _newFeatureValues[fe.environment!.id]?.valueString = null;
    _newFeatureValues[fe.environment!.id]?.valueJson = null;
    _newFeatureValues[fe.environment!.id]?.valueNumber = null;
  }

  Future<Environment> getEnvironment(String envId) async {
    return _environmentServiceApi
        .getEnvironment(envId,
            includeServiceAccounts: true, includeSdkUrl: true)
        .catchError((e, s) {
      mrClient.dialogError(e, s);
    });
  }

  void reset() {
    _originalFeatureValues.forEach((key, value) {
      final original = value.copyWith();
      _newFeatureValues[key] = original;
    });
  }

  saveFeatureValueUpdates() async {
    await _featureServiceApi.updateAllFeatureValuesByApplicationForKey(
        applicationId, feature.key!, [currentFeatureValue!]);
    await _featureStatusBloc.updateApplicationFeatureValuesStream();

  }
}
