import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

import 'feature_status_bloc.dart';

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

class FeatureValuesBloc implements Bloc {
  final Feature feature;
  final String applicationId;
  final ManagementRepositoryClientBloc mrClient;
  EnvironmentServiceApi _environmentServiceApi;
  // environment id, FeatureValue - there may be values in here that are not used, we honour `_dirty` to determine if we use them
  final _newFeatureValues = <String, FeatureValue>{};
  final _originalFeatureValues = <String, FeatureValue>{};
  final _fvUpdates = <String, FeatureValue>{};
  final _fvLockedUpdates = <String, BehaviorSubject<bool>>{};
  final ApplicationFeatureValues applicationFeatureValues;
  final FeatureStatusBloc _featureStatusBloc;

  // environmentId, true/false (if dirty)
  final _dirty = <String, bool>{};
  final _dirtyLock = <String, bool>{};
  final _dirtyValues = <String, FeatureValueDirtyHolder>{};

  // if any of the values are updated, this stream shows true, it can flick on and off during its lifetime
  final _dirtyBS = BehaviorSubject<bool>();
  Stream<bool> get anyDirty => _dirtyBS.stream;

  // provides back a stream of updates to any listener for this cell
  FeatureValue featureValueByEnvironment(String envId) {
    return _fvUpdates.putIfAbsent(envId, () {
      final fv = _newFeatureValues.putIfAbsent(
          envId,
          () => FeatureValue()
            ..environmentId = envId
            ..locked = false
            ..key = feature.key);

      return fv;
    });
  }

  BehaviorSubject<bool> _environmentIsLocked(String envId) {
    return _fvLockedUpdates.putIfAbsent(envId, () {
      final fv = featureValueByEnvironment(envId);
      return BehaviorSubject<bool>.seeded(fv.locked);
    });
  }

  // the cells control their own state, but they are affected by whether they become locked or unlocked while they
  // exist
  Stream<bool> environmentIsLocked(String envId) {
    return _environmentIsLocked(envId);
  }

  void dirtyLock(String envId, bool newLock) {
    final original = _originalFeatureValues[envId];
    final newValue = featureValueByEnvironment(envId);
    newValue.locked = newLock;

    // is the old and new value different?
    final newDirty = newValue.locked != (original?.locked ?? false);

    // is the new changed value different from the old changed value?
    if (newDirty != _dirtyLock[envId]) {
      _dirtyLock[envId] = newDirty;
      _environmentIsLocked(envId).add(newLock);
      _dirtyCheck();
    }
  }

  void _dirtyCheck() {
    _dirtyBS.add(_dirty.values.any((d) => d == true) ||
        _dirtyLock.values.any((d) => d == true));
  }

  bool dirty(String envId, DirtyFeatureHolderCallback dirtyValueCallback) {
    final original = _originalFeatureValues[envId];
    final current = _dirtyValues[envId] ??
        (FeatureValueDirtyHolder()
          ..value = _originalValue(original)
          ..customStrategies = original.rolloutStrategies
          ..sharedStrategies = original.rolloutStrategyInstances);
    dirtyValueCallback(current);
    _dirtyValues[envId] = current;

    _dirty[envId] = false;

    if (_originalValue(original) != current.value) {
      _dirty[envId] = true;
    }

    if (!(ListEquality()
        .equals(original.rolloutStrategies, current.customStrategies))) {
      _dirty[envId] = true;
    }

    _dirtyCheck();

    return _dirty[envId];
  }

  dynamic _originalValue(FeatureValue original) {
    switch (feature.valueType) {
      case FeatureValueType.BOOLEAN:
        return original.valueBoolean;
      case FeatureValueType.STRING:
        return original.valueString;
      case FeatureValueType.NUMBER:
        return original.valueNumber;
      case FeatureValueType.JSON:
        return original.valueJson;
    }

    return null;
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
    _fvLockedUpdates.values.forEach((element) {
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
      _fvLockedUpdates[key].add(original.locked);
    });

    _dirty.clear();
    _dirtyValues.clear();
    _dirtyLock.clear();
    _dirtyBS.add(false);
  }

  void _updateNewFeature(
      FeatureValue newValue, FeatureValue value, String envId) {
    if (_dirtyLock[envId] == true) {
      newValue.locked = !(value?.locked ?? false);
    }

    if (_dirty[envId] == true) {
      final fvDirty = _dirtyValues[envId];

      switch (feature.valueType) {
        case FeatureValueType.BOOLEAN:
          newValue.valueBoolean = fvDirty.value;
          break;
        case FeatureValueType.STRING:
          newValue.valueString = fvDirty.value;
          break;
        case FeatureValueType.NUMBER:
          newValue.valueNumber = fvDirty.value;
          break;
        case FeatureValueType.JSON:
          newValue.valueJson = fvDirty.value;
          break;
      }

      newValue.rolloutStrategies = fvDirty.customStrategies;
      newValue.rolloutStrategyInstances = fvDirty.sharedStrategies;
    }
  }

  Future<bool> updateDirtyStates() async {
    final updates = <FeatureValue>[];

    // this represents all feature values that are currently being shown,
    // we don't know if they have new values or not
    final featureValuesWeAreCheckingForUpdates = <String, FeatureValue>{}
      ..addAll(_newFeatureValues);

    _originalFeatureValues.forEach((envId, value) {
      final newValue = featureValuesWeAreCheckingForUpdates
          .remove(envId); // we have no access, ignore it

      // this causes equals not to work
      newValue.whoUpdated = null;
      value.whoUpdated = null;

      if (_dirty[envId] == true || _dirtyLock[envId] == true) {
        final roles = applicationFeatureValues.environments
            .firstWhere((e) => e.environmentId == envId)
            .roles;
        // do we have access to change it, is it different?
        if ((roles.contains(RoleType.CHANGE_VALUE) ||
            roles.contains(RoleType.LOCK) ||
            roles.contains(RoleType.UNLOCK))) {
          _updateNewFeature(newValue, value, envId);

          updates.add(newValue);
        }
      }
    });

    // anything else in the new values list is stuff we didn't have originally
    featureValuesWeAreCheckingForUpdates.values.forEach((newFv) {
      final roles = applicationFeatureValues.environments
          .firstWhere((e) => e.environmentId == newFv.environmentId)
          .roles;
      if (roles.contains(RoleType.CHANGE_VALUE) ||
          roles.contains(RoleType.LOCK) ||
          roles.contains(RoleType.UNLOCK)) {
        // only add the ones where we set locked away from its default (false) or set a value
        if (_dirty[newFv.environmentId] == true ||
            _dirtyLock[newFv.environmentId] == true) {
          _updateNewFeature(newFv, null, newFv.environmentId);
          updates.add(newFv);
        }
      }
    });

    // TODO: catching of error, reporting of dialog
    if (updates.isNotEmpty) {
      await _featureStatusBloc.updateAllFeatureValuesByApplicationForKey(
          feature, updates);
    }

    _dirtyBS.add(false);

    return true;
  }
}
