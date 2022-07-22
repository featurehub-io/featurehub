import 'dart:async';
import 'dart:math';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/features/feature_dashboard_constants.dart';
import 'package:open_admin_app/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:rxdart/rxdart.dart';

import 'per_application_features_bloc.dart';

enum TabsState { featureFlags, featureValues, configurations }

class FeatureStrategyCountOverride {
  final Feature feature;
  final String environmentId;
  int strategyCount;

  FeatureStrategyCountOverride(
      this.feature, this.environmentId, this.strategyCount);

  @override
  String toString() {
    return 'FeatureStrategyCountOverride{feature: $feature, environmentId: $environmentId, strategyCount: $strategyCount}';
  }
}

class CombinedEnvironmentInfoAndFeaturesEditing {
  final Set<String>? featureKeys;
  final EnvironmentsInfo environmentsInfo;

  CombinedEnvironmentInfoAndFeaturesEditing(this.featureKeys,
      this.environmentsInfo);

  @override
  String toString() {
    return 'CombinedEnvironmentInfoAndFeaturesEditing{featureKeys: $featureKeys, environmentsInfo: $environmentsInfo}';
  }
}

// this bloc is actually mixing the feature groups available and features???
class FeaturesOnThisTabTrackerBloc implements Bloc {
  final FeatureGrouping grouping;
  final List<Feature> _featuresForTabs = [];
  final _currentlyEditingFeatureKeys = <String>{};
  final _featureCurrentlyEditingSource = BehaviorSubject.seeded(<String>{});
  final ManagementRepositoryClientBloc mrClient;
  final _allFeaturesByKey = <String, Feature>{};
  String? filter;
  int startingFeatureIndex = 0;

  Stream<FeaturesByType> featuresStream;

  Stream<Set<String>?> get featureCurrentlyEditingStream =>
      _featureCurrentlyEditingSource.stream;

  late Stream<CombinedEnvironmentInfoAndFeaturesEditing> environmentAndFeaturesCurrentlyEditingStream;

  // when editing, the base count we have doesn't match what is actually being used
  // so we need to override it. We also need to clean it up on save or edit
  final List<FeatureStrategyCountOverride>
      _featurePerEnvironmentStrategyCountOverrides = [];

  List<Feature> get features => _featuresForTabs;
  final featureValueBlocs = <String, PerFeatureStateTrackingBloc>{};
  final PerApplicationFeaturesBloc featureStatusBloc;
  late StreamSubscription<FeaturesByType?> _featureStreamSubscription;
  late StreamSubscription<Feature> _newFeatureStream;

  ApplicationFeatureValues applicationFeatureValues;

  FeaturesOnThisTabTrackerBloc(this.grouping,
      this.featureStatusBloc) :
        featuresStream = featureStatusBloc.appFeatures(grouping),
        mrClient = featureStatusBloc.mrClient,
        applicationFeatureValues = FeaturesByType.empty(grouping).applicationFeatureValues
  {
    environmentAndFeaturesCurrentlyEditingStream =
      Rx.combineLatest2(featureCurrentlyEditingStream, featureStatusBloc.environmentsStream, (Set<String>? features, EnvironmentsInfo envs) =>
          CombinedEnvironmentInfoAndFeaturesEditing(features, envs));

    _featureStreamSubscription = featuresStream.listen((appFeatures) {
      applicationFeatureValues = appFeatures.applicationFeatureValues;

      if (appFeatures.isEmpty) {
        // we have just swapped to an application, so we need to request the list of features
        featureStatusBloc.updateFeatureGrouping(grouping, filter, startingFeatureIndex);

        _allFeaturesByKey.clear();
        _featuresForTabs.clear();
      } else {
        _featuresForTabs.clear();
        _featuresForTabs.addAll(appFeatures.applicationFeatureValues.features);

        _refixFeaturesByKey(appFeatures);

        _checkForFeaturesWeWereEditingThatHaveNowGone();
      }
    });

    _newFeatureStream =
        featureStatusBloc.publishNewFeatureStream.listen((feature) {
          if (grouping.types.contains(feature.valueType)) {
            _currentlyEditingFeatureKeys.add(feature.key!);
          }
    });
  }

  void addFeatureEnvironmentStrategyCountOverride(
      FeatureStrategyCountOverride fsco) {
    final found = _featurePerEnvironmentStrategyCountOverrides.firstWhereOrNull(
        (e) =>
            e.environmentId == fsco.environmentId &&
            e.feature.id == fsco.feature.id);

    if (found == null || found.strategyCount != fsco.strategyCount) {
      if (found == null) {
        _featurePerEnvironmentStrategyCountOverrides.add(fsco);
      } else {
        found.strategyCount = fsco.strategyCount;
      }
    }
  }

  void _cleanFeatureStrategyCountOverridesOnSaveOrCancel(Feature feature) {
    _featurePerEnvironmentStrategyCountOverrides
        .removeWhere((e) => e.feature.id == feature.id);
  }

  double featureExtraCellHeight(Feature feature) {
    final selected = _currentlyEditingFeatureKeys.contains(feature.key);

    final maxRowsForFeature = _totalStrategyLines([feature]);

    if (selected) {
      return maxRowsForFeature * selectedRowHeightPerStrategy;
    } else {
      return maxRowsForFeature * unselectedRowHeightPerStrategy;
    }
  }

  int get unselectedFeatureCount => _featuresForTabs
      .where((f) => !_currentlyEditingFeatureKeys.contains(f.key))
      .length;

  int _strategyLines(FeatureValue? fv) {
    if (fv == null) return 0;
    final rsLen = fv.rolloutStrategies.length;
    final rsiLen = fv.rolloutStrategyInstances.length;
    return rsLen + rsiLen;
  }

  int _totalStrategyLines(List<Feature> sel) {
    // go through the features on this tab and find those we are NOT
    // currently interested in

    if (sel.isEmpty) {
      return 0;
    }

    final fvKeys = sel.map((e) => e.key).toList();

    // now we go through each environment sideways, finding the one with
    // the highest rows and then summing them all

    // pull the overrides out for this set of features
    final overridesForFeatures = _featurePerEnvironmentStrategyCountOverrides
        .where((e) => fvKeys.contains(e.feature.key))
        .toList();

    var linesInAllFeatures = applicationFeatureValues.environments
        .where((env) => featureStatusBloc.environmentVisible(env.environmentId!))
        .map((e) {
      // this will only pick up where we have actual features,
      // if we have an environment with a null feature value and
      // we start editing it, we will have to pull this from
      // the overrides (if any)

      var map = fvKeys.map((key) {
        final uneditedFeat = e.features.firstWhereOrNull((fv) => fv.key == key);
        final editedFeat = overridesForFeatures.firstWhereOrNull(
            (x) => x.environmentId == e.environmentId && x.feature.key == key);
        if (editedFeat != null) {
          return editedFeat.strategyCount;
        }

        return _strategyLines(uneditedFeat);
      });

      // this is the count of all of the features indicated going across
      // this one environment. This effectively finds the maximum size spacing
      // for this specific environment
      return map.isEmpty ? 0 : map.reduce((a, b) => a + b);
    });

    final maxLinesInAllFeatures =
        linesInAllFeatures.isEmpty ? 0 : linesInAllFeatures.reduce(max);

    return maxLinesInAllFeatures;
  }

  /// this gives the total height for the entire table based on
  /// what tab we are on and what environments we have hidden
  double get unselectedFeatureCountForHeight {
    // go through the features on this tab and find those we are NOT
    // currently editing
    final sel = _featuresForTabs
        .where((f) => !_currentlyEditingFeatureKeys.contains(f.key))
        .toList();

    final maxLinesInAllFeatures = sel.isEmpty
        ? 0
        : sel.map((f) => _totalStrategyLines([f])).reduce((a, b) => a + b);

    final retVal = (maxLinesInAllFeatures * unselectedRowHeightPerStrategy) +
        (unselectedRowHeight * sel.length);

    return retVal;
  }

  int get selectedFeatureCount => _featuresForTabs
      .where((f) => _currentlyEditingFeatureKeys.contains(f.key))
      .length;

  double get selectedFeatureCountForHeight {
    final sel = _featuresForTabs
        .where((f) => _currentlyEditingFeatureKeys.contains(f.key))
        .toList();

    final maxLinesInAllFeatures = sel.isEmpty
        ? 0
        : sel.map((f) => _totalStrategyLines([f])).reduce((a, b) => a + b);

    final retVal = (maxLinesInAllFeatures * selectedRowHeightPerStrategy) +
        (selectedRowHeight * sel.length);

    return retVal;
  }

  // turns them into a map for easy access
  void _refixFeaturesByKey(FeaturesByType appFeatures) {
    _allFeaturesByKey.clear();

    for (var f in appFeatures.applicationFeatureValues.features) {
      _allFeaturesByKey[f.key!] = f;
    }
  }

  // List<EnvironmentFeatureValues> get sortedEnvironmentsThatAreShowing {
  //   final shownEnvs = featureStatusBloc.
  //   return featureGrouping.sortedByNameEnvironmentIds
  //       .where((id) => shownEnvironments.contains(id))
  //       .map((id) => featureGrouping.applicationEnvironments[id])
  //       .whereNotNull()
  //       .toList();
  // }

  @override
  void dispose() {
    // clean up any outstanding value blocs
    for (var element in featureValueBlocs.values) {
      element.dispose();
    }

    _featureStreamSubscription.cancel();
    _newFeatureStream.cancel();
  }

  void hideOrShowFeature(Feature feature) {
    final val = _currentlyEditingFeatureKeys;

    if (!val.contains(feature.key)) {
      _createFeatureValueBlocForFeature(feature);
      val.add(feature.key!);
    } else {
      final featureValueBloc = featureValueBlocs[feature.key];
      if (featureValueBloc != null) {
        featureValueBloc.dispose();
      }
      featureValueBlocs.remove(featureValueBloc);
      val.remove(feature.key);
    }

    _featureCurrentlyEditingSource.add(val);
  }

  void _createFeatureValueBlocForFeature(Feature feature) {
    var values = applicationFeatureValues.environments
        .map((env) =>
            env.features.firstWhereOrNull((fv) => fv.key == feature.key))
        .whereNotNull()
        .toList();

    featureValueBlocs[feature.key!] = PerFeatureStateTrackingBloc(
        featureStatusBloc.applicationId!,
        feature,
        mrClient,
        this,
        values,
        featureStatusBloc,
        applicationFeatureValues);
  }

  Feature findFeature(String key, String environmentId) {
    return _allFeaturesByKey[key]!;
  }

  void _checkForFeaturesWeWereEditingThatHaveNowGone() {
    final removeMissing = _currentlyEditingFeatureKeys;

    removeMissing.where((key) {
      final remove = _allFeaturesByKey[key] == null;
      if (remove) {
        // stop tracking any override sizes for this feature
        _cleanFeatureStrategyCountOverridesOnSaveOrCancel(
            featureValueBlocs[key]!.feature);
        // get rid of the block
        featureValueBlocs[key]!.dispose();
        featureValueBlocs.remove(key);
      }
      return remove;
    });

    // now make sure all features are there as
    // when we create one we can add it to the list and it not have a bloc
    for (var key in removeMissing) {
      if (featureValueBlocs[key] == null) {
        _createFeatureValueBlocForFeature(_allFeaturesByKey[key]!);
      }
    }

    // trigger a rebuild
    _featureCurrentlyEditingSource.add(removeMissing);
  }
}
