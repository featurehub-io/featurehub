import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

import 'feature_status_bloc.dart';
import 'feature_value_status_tags.dart';

typedef DirtyCallback = bool Function(FeatureValue original);

class FeatureValuesBloc implements Bloc {
  final Feature feature;
  final String applicationId;
  final ManagementRepositoryClientBloc mrClient;
  EnvironmentServiceApi _environmentServiceApi;
  // environment id, FeatureValue - there may be values in here that are not used, we honour `_dirty` to determine if we use them
  final _newFeatureValues = <String, FeatureValue>{};
  final _originalFeatureValues = <String, FeatureValue>{};
  final _fvUpdates = <String, BehaviorSubject<FeatureValue>>{};
  final ApplicationFeatureValues applicationFeatureValues;
  final FeatureStatusBloc _featureStatusBloc;

  // environmentId, true/false (if dirty)
  final _dirty = <String, bool>{};

  // if any of the values are updated, this stream shows true, it can flick on and off during its lifetime
  final _dirtyBS = BehaviorSubject<bool>();
  Stream<bool> get anyDirty => _dirtyBS.stream;

  // provides back a stream of updates to any listener for this cell
  Stream<FeatureValue> featureValueByEnvironment(String envId) {
    return _fvUpdates.putIfAbsent(envId, () {
      final fv = _newFeatureValues.putIfAbsent(
          envId,
          () => FeatureValue()
            ..environmentId = envId
            ..locked = false
            ..key = feature.key);

      return BehaviorSubject<FeatureValue>.seeded(fv);
    });
  }

  // we have handed the client a mutable object, so this is just them telling us that it has been changed
  // within their purview. This lets any other listening view object update itself
  void updatedFeature(String envId) {
    // check to see if it actually changed
    final original = _originalFeatureValues[envId];
    final fv = _newFeatureValues[envId];

    _dirty[envId] = (original != fv); // equals is implemented
    _dirtyBS.add(_dirty.values.any((d) => d == true));

    _fvUpdates[envId].add(_newFeatureValues[envId]);
  }

  void dirty(String envId, DirtyCallback originalCheck) {
    _dirty[envId] = originalCheck(_originalFeatureValues[envId]);
    _dirtyBS.add(_dirty.values.any((d) => d == true));
  }

  FeatureValuesBloc(
      this.applicationId,
      this.feature,
      this.mrClient,
      List<FeatureValue> featureValuesThisFeature,
      FeatureStatusBloc featureStatusBloc,
      this.applicationFeatureValues)
      : assert(applicationFeatureValues != null),
        assert(featureStatusBloc != null),
        _featureStatusBloc = featureStatusBloc,
        assert(mrClient != null) {
    _environmentServiceApi = EnvironmentServiceApi(mrClient.apiClient);
    // lets get this party started

    featureValuesThisFeature.forEach((fv) {
      // make a copy so our changes don't leak back into the main list
      _newFeatureValues[fv.environmentId] = fv.copyWith();
      _originalFeatureValues[fv.environmentId] = fv.copyWith();
    });
  }

  @override
  void dispose() {
    _fvUpdates.values.forEach((element) {
      element.close();
    });
  }

  bool hasValue(FeatureEnvironment fe) {
    return _newFeatureValues[fe.environment.id]?.valueBoolean != null ||
        _newFeatureValues[fe.environment.id]?.valueString != null ||
        _newFeatureValues[fe.environment.id]?.valueJson != null ||
        _newFeatureValues[fe.environment.id]?.valueNumber != null;
  }

  void resetValue(FeatureEnvironment fe) {
    _newFeatureValues[fe.environment.id]?.valueBoolean = null;
    _newFeatureValues[fe.environment.id]?.valueString = null;
    _newFeatureValues[fe.environment.id]?.valueJson = null;
    _newFeatureValues[fe.environment.id]?.valueNumber = null;
  }

  Future<Environment> getEnvironment(String envId) async {
    return _environmentServiceApi
        .getEnvironment(envId,
            includeServiceAccounts: true, includeSdkUrl: true)
        .catchError(mrClient.dialogError);
  }

  void reset() {
    _originalFeatureValues.forEach((key, value) {
      final original = value.copyWith();
      _newFeatureValues[key] = original;
      _fvUpdates[key].add(original);
    });

    _dirty.clear();
    _dirtyBS.add(false);
  }

  Future<bool> updateDirtyStates() async {
    final updates = <FeatureValue>[];
    final featureValuesWeAreCheckingForUpdates = <String, FeatureValue>{}
      ..addAll(_newFeatureValues);

    _originalFeatureValues.forEach((envId, value) {
      final roles = applicationFeatureValues.environments
          .firstWhere((e) => e.environmentId == envId)
          .roles;
      if (roles.contains(RoleType.CHANGE_VALUE) ||
          roles.contains(RoleType.LOCK) ||
          roles.contains(RoleType.UNLOCK)) {
        final _newFeatureValue = featureValuesWeAreCheckingForUpdates[envId];
        if (_newFeatureValue != null) {
          featureValuesWeAreCheckingForUpdates.remove(
              envId); // we are then left with ones that just have new data

          if (_newFeatureValue != value) {
            updates.add(_newFeatureValue);
          }
        }
      }
    });

    featureValuesWeAreCheckingForUpdates.values.forEach((newFv) {
      final roles = applicationFeatureValues.environments
          .firstWhere((e) => e.environmentId == newFv.environmentId)
          .roles;
      if (roles.contains(RoleType.CHANGE_VALUE) ||
          roles.contains(RoleType.LOCK) ||
          roles.contains(RoleType.UNLOCK)) {
        // only add the ones where we set locked away from its default (false) or set a value
        if (newFv.locked || newFv.isSet(feature)) {
          updates.add(newFv);
        }
      }
    });

    // TODO: catching of error, reporting of dialog
    await _featureStatusBloc.updateAllFeatureValuesByApplicationForKey(
        feature, updates);

    _dirtyBS.add(false);

    return true;
  }
}
