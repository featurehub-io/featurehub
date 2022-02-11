import 'dart:async';
import 'dart:math';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/features/custom_strategy_bloc.dart';
import 'package:open_admin_app/widgets/features/tabs_bloc.dart';
import 'package:rxdart/rxdart.dart';

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
  final ManagementRepositoryClientBloc mrClient;
  late EnvironmentServiceApi _environmentServiceApi;
  // environment id, FeatureValue - there may be values in here that are not used, we honour `_dirty` to determine if we use them
  final _newFeatureValues = <String, FeatureValue>{};
  final _originalFeatureValues = <String, FeatureValue>{};
  final _fvUpdates = <String, FeatureValue>{};
  final _fvLockedUpdates = <String, BehaviorSubject<bool>>{};
  final _fvRetiredUpdates = <String, BehaviorSubject<bool>>{};
  final ApplicationFeatureValues applicationFeatureValues;
  final PerApplicationFeaturesBloc _featureStatusBloc;
  final FeaturesOnThisTabTrackerBloc featuresOnTabBloc;
  final _customStrategyBlocs = <EnvironmentFeatureValues, CustomStrategyBloc>{};

  // environmentId, true/false (if dirty)
  final _dirty = <String, bool>{};
  final _dirtyLock = <String, bool>{};
  final _dirtyRetired = <String, bool>{};
  final _dirtyValues = <String, FeatureValueDirtyHolder>{};

  CustomStrategyBloc matchingCustomStrategyBloc(EnvironmentFeatureValues efv) {
    return _customStrategyBlocs.putIfAbsent(
        efv, () => CustomStrategyBloc(efv, feature, this));
  }

  int get maxLines => _dirtyValues.values
      .map((e) => (e.customStrategies.length) + (e.sharedStrategies.length))
      .reduce(max);

  // if any of the values are updated, this stream shows true, it can flick on and off during its lifetime
  final _dirtyBS = BehaviorSubject<bool>();
  Stream<bool> get anyDirty => _dirtyBS.stream;

  PerApplicationFeaturesBloc get perApplicationFeaturesBloc =>
      _featureStatusBloc;

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

  BehaviorSubject<bool> _environmentIsLocked(String envId) {
    return _fvLockedUpdates.putIfAbsent(envId, () {
      final fv = featureValueByEnvironment(envId);
      return BehaviorSubject<bool>.seeded(fv.locked);
    });
  }

  BehaviorSubject<bool> _isFeatureValueRetired(String envId) {
    return _fvRetiredUpdates.putIfAbsent(envId, () {
      final fv = featureValueByEnvironment(envId);
      return BehaviorSubject<bool>.seeded(fv.retired == true);
    });
  }

  // the cells control their own state, but they are affected by whether they become locked or unlocked while they
  // exist
  Stream<bool> environmentIsLocked(String envId) {
    return _environmentIsLocked(envId);
  }

  Stream<bool> isFeatureValueRetired(String envId) {
    return _isFeatureValueRetired(envId);
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

  void dirtyRetired(String envId, bool newRetired) {
    final original = _originalFeatureValues[envId];
    final newValue = featureValueByEnvironment(envId);
    newValue.retired = newRetired;

    // is the old and new value different?
    final newDirty = newValue.retired != (original?.retired ?? false);

    // is the new changed value different from the old changed value?
    if (newDirty != _dirtyRetired[envId]) {
      _dirtyRetired[envId] = newDirty;
      _isFeatureValueRetired(envId).add(newRetired);
      _dirtyCheck();
    }
  }

  void _dirtyCheck() {
    _dirtyBS.add(_dirty.values.any((d) => d == true) ||
        _dirtyLock.values.any((d) => d == true) ||
        _dirtyRetired.values.any((d) => d == true));
  }

  bool dirty(String envId, DirtyFeatureHolderCallback dirtyValueCallback) {
    final original = _originalFeatureValues[envId];
    final current = _dirtyValues[envId] ??
        (FeatureValueDirtyHolder()
          ..value = _originalValue(original)
          ..customStrategies =
              (original?.rolloutStrategies ?? <RolloutStrategy>[])
          ..sharedStrategies = (original?.rolloutStrategyInstances) ??
              <RolloutStrategyInstance>[]);
    dirtyValueCallback(current);
    _dirtyValues[envId] = current;

    _dirty[envId] = false;

    if (original == null &&
        (current.value != null ||
            current.customStrategies.isNotEmpty ||
            current.sharedStrategies.isNotEmpty)) {
      _dirty[envId] = true;
    } else if (original != null) {
      if (_originalValue(original) != current.value) {
        _dirty[envId] = true;
      } else if (!(const ListEquality()
          .equals(original.rolloutStrategies, current.customStrategies))) {
        _dirty[envId] = true;
      }
    }

    featuresOnTabBloc.addFeatureEnvironmentStrategyCountOverride(
        FeatureStrategyCountOverride(
            feature, envId, current.customStrategies.length));

    _dirtyCheck();

    return _dirty[envId]!;
  }

  dynamic _originalValue(FeatureValue? original) {
    if (original != null) {
      switch (feature.valueType!) {
        case FeatureValueType.BOOLEAN:
          return original.valueBoolean;
        case FeatureValueType.STRING:
          return original.valueString;
        case FeatureValueType.NUMBER:
          return original.valueNumber;
        case FeatureValueType.JSON:
          return original.valueJson;
      }
    }

    return null;
  }

  PerFeatureStateTrackingBloc(
      this.applicationId,
      this.feature,
      this.mrClient,
      this.featuresOnTabBloc,
      List<FeatureValue> featureValuesThisFeature,
      PerApplicationFeaturesBloc featureStatusBloc,
      this.applicationFeatureValues)
      : _featureStatusBloc = featureStatusBloc {
    _environmentServiceApi = EnvironmentServiceApi(mrClient.apiClient);
    // lets get this party started

    for (var fv in featureValuesThisFeature) {
      // make a copy so our changes don't leak back into the main list
      _newFeatureValues[fv.environmentId!] = fv.copyWith();
      _originalFeatureValues[fv.environmentId!] = fv.copyWith();
      _dirtyValues[fv.key] = FeatureValueDirtyHolder()
        ..value = fv
        ..customStrategies = fv.rolloutStrategies
        ..sharedStrategies = fv.rolloutStrategyInstances;
    }
  }

  @override
  void dispose() {
    for (var element in _fvLockedUpdates.values) {
      element.close();
    }

    for (var element in _fvRetiredUpdates.values) {
      element.close();
    }

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
      _fvLockedUpdates[key]!.add(original.locked);
    });

    _dirty.clear();
    _dirtyValues.clear();
    _dirtyLock.clear();
    _dirtyRetired.clear();
    _dirtyBS.add(false);
  }

  // this takes the changes we are caching in our: dirtyValues, dirty lock, dirty retired
  // and put them into the new feature value. Each item that has a UI element is kept track
  // of separately.
  void _updateNewFeatureValueWithDirtyChanges(
      FeatureValue newValue, FeatureValue? value, String envId) {
    if (_dirtyLock[envId] == true) {
      newValue.locked = !(value?.locked ?? false);
    }
    if (_dirtyRetired[envId] == true) {
      newValue.retired = !(value?.retired ??
          false); // if the original was true and its dirty, it has to be false and vs versa
    }

    if (_dirty[envId] == true) {
      final fvDirty = _dirtyValues[envId]!;

      switch (feature.valueType!) {
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
          .remove(envId)!; // we have no access, ignore it

      // this causes equals not to work
      newValue.whoUpdated = null;
      value.whoUpdated = null;

      if (_dirty[envId] == true ||
          _dirtyLock[envId] == true ||
          _dirtyRetired[envId] == true) {
        final roles = applicationFeatureValues.environments
            .firstWhere((e) => e.environmentId == envId)
            .roles;
        // do we have access to change it, is it different?
        if ((roles.contains(RoleType.CHANGE_VALUE) ||
            roles.contains(RoleType.LOCK) ||
            roles.contains(RoleType.UNLOCK))) {
          _updateNewFeatureValueWithDirtyChanges(newValue, value, envId);

          updates.add(newValue);
        }
      }
    });

    // anything else in the new values list is stuff we didn't have originally
    for (var newFv in featureValuesWeAreCheckingForUpdates.values) {
      final roles = applicationFeatureValues.environments
          .firstWhere((e) => e.environmentId == newFv.environmentId)
          .roles;
      if (roles.contains(RoleType.CHANGE_VALUE) ||
          roles.contains(RoleType.LOCK) ||
          roles.contains(RoleType.UNLOCK)) {
        // only add the ones where we set locked away from its default (false) or set a value
        if (_dirty[newFv.environmentId] == true ||
            _dirtyLock[newFv.environmentId] == true ||
            _dirtyRetired[newFv.environmentId] == true) {
          _updateNewFeatureValueWithDirtyChanges(
              newFv, null, newFv.environmentId!);
          updates.add(newFv);
        }
      }
    }

    // TODO: catching of error, reporting of dialog
    if (updates.isNotEmpty) {
      await _featureStatusBloc.updateAllFeatureValuesByApplicationForKey(
          feature, updates);
    }

    _dirtyBS.add(false);

    return true;
  }
}
