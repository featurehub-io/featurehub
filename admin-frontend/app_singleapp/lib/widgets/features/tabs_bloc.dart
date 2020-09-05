import 'dart:async';
import 'dart:math';

import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/widgets/features/feature_dashboard_constants.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

import 'feature_status_bloc.dart';
import 'feature_values_bloc.dart';

enum TabsState { FLAGS, VALUES, CONFIGURATIONS }

class TabsBloc implements Bloc {
  final String applicationId;
  FeatureStatusFeatures featureStatus;
  final _stateSource = BehaviorSubject<TabsState>.seeded(TabsState.FLAGS);
  List<Feature> _featuresForTabs;
  final _currentlyEditingFeatureKeys = <String>{};
  final _featureCurrentlyEditingSource = BehaviorSubject<Set<String>>();
  final ManagementRepositoryClientBloc mrClient;
  final _allFeaturesByKey = <String, Feature>{};

  // determine which tab they have selected
  Stream<TabsState> get currentTab => _stateSource.stream;

  Stream<Set<String>> get featureCurrentlyEditingStream =>
      _featureCurrentlyEditingSource.stream;

  List<Feature> get features => _featuresForTabs;
  final featureValueBlocs = <String, FeatureValuesBloc>{};
  final FeatureStatusBloc featureStatusBloc;
  StreamSubscription<FeatureStatusFeatures> _featureStream;
  StreamSubscription<Feature> _newFeatureStream;
  StreamSubscription<List<String>> _shownEnvironmentsStream;

  var shownEnvironments = <String>[];

  TabsBloc(this.featureStatus, this.applicationId, this.mrClient,
      this.featureStatusBloc)
      : assert(featureStatus != null),
        assert(featureStatusBloc != null),
        assert(applicationId != null) {
    _featureCurrentlyEditingSource.add(_currentlyEditingFeatureKeys);

    // if they have created a new feature we want to swap to the right tab
    _fixFeaturesForTabs(_stateSource.value);
    _refixFeaturesByKey();

    _featureStream = featureStatusBloc.appFeatureValues.listen((appFeatures) {
      if (appFeatures != null) {
        featureStatus = appFeatures;

        _fixFeaturesForTabs(_stateSource.value);
        _refixFeaturesByKey();

        _checkForFeaturesWeWereEditingThatHaveNowGone();
      }
    });

    _shownEnvironmentsStream = featureStatusBloc.shownEnvironmentsStream
        .listen(_updatedShownEnvironments);

    _newFeatureStream =
        featureStatusBloc.publishNewFeatureStream.listen((feature) {
      switch (feature.valueType) {
        case FeatureValueType.BOOLEAN:
          _stateSource.value = TabsState.FLAGS;
          break;
        case FeatureValueType.STRING:
          _stateSource.value = TabsState.VALUES;
          break;
        case FeatureValueType.NUMBER:
          _stateSource.value = TabsState.VALUES;
          break;
        case FeatureValueType.JSON:
          _stateSource.value = TabsState.CONFIGURATIONS;
          break;
      }

      _currentlyEditingFeatureKeys.add(feature.key);
    });
  }

  void _updatedShownEnvironments(List<String> environments) {
    shownEnvironments = environments;
  }

  double featureExtraCellHeight(Feature feature) {
    final selected = _currentlyEditingFeatureKeys.contains(feature.key);

    if (selected) {
      return featureValueBlocs.values.map((e) => e.maxLines).reduce(max) *
          selectedRowHeightPerStrategy;
    } else {
      // find the max rows for this feature
      final maxRowsForFeature =
          (featureStatus.applicationFeatureValues.environments.map((e) {
        final fv = e.features
            .firstWhere((fw) => fw.key == feature.key, orElse: () => null);

        return (fv == null)
            ? 0
            : (fv.rolloutStrategies.length +
                fv.rolloutStrategyInstances.length);
      }).reduce(max));

      return maxRowsForFeature * unselectedRowHeightPerStrategy;
    }
  }

  int get unselectedFeatureCount => _featuresForTabs
      .where((f) => !_currentlyEditingFeatureKeys.contains(f.key))
      .length;

  /// this gives the total height for the entire table based on
  /// what tab we are on and what environments we have hidden
  double get unselectedFeatureCountForHeight {
    // go through the features on this tab and find those we are NOT
    // currently editing
    final sel = _featuresForTabs
        .where((f) => !_currentlyEditingFeatureKeys.contains(f.key));
    if (sel.isEmpty) {
      return 0;
    }
    final unselectedKeys = sel.map((e) => e.key).toList();
    // go through all environments looking for this feature, and where
    // there are feature values, figure out how many lines they will take
    final maxLinesInAllFeatures =
        featureStatus.applicationFeatureValues.environments.map((e) {
      final fv = e.features.firstWhere(
          (fw) => fw.key != null && unselectedKeys.contains(fw.key),
          orElse: () => null);

      final r = (fv == null)
          ? 0
          : (fv.rolloutStrategies.length + fv.rolloutStrategyInstances.length);
      return r;
    }).reduce(max);

    final retVal = (maxLinesInAllFeatures * unselectedRowHeightPerStrategy) +
        unselectedRowHeight;
    return retVal;
  }

  int get selectedFeatureCount => _featuresForTabs
      .where((f) => _currentlyEditingFeatureKeys.contains(f.key))
      .length;

  double get selectedFeatureCountForHeight {
    final sel = _featuresForTabs
        .where((f) => _currentlyEditingFeatureKeys.contains(f.key));
    if (sel.isEmpty) {
      return 0;
    }

    return sel
                .map((e) =>
                    featureValueBlocs.values.map((e) => e.maxLines).reduce(max))
                .reduce((a, b) => a + b) *
            selectedRowHeightPerStrategy +
        selectedRowHeight;
  }

  // turns them into a map for easy access
  void _refixFeaturesByKey() {
    featureStatus.applicationFeatureValues.features.forEach((f) {
      _allFeaturesByKey[f.key] = f;
    });
  }

  void _fixFeaturesForTabs(TabsState tab) {
    _featuresForTabs =
        featureStatus.applicationFeatureValues.features.where((f) {
      return ((tab == TabsState.FLAGS) &&
              f.valueType == FeatureValueType.BOOLEAN) ||
          ((tab == TabsState.VALUES) &&
              (f.valueType == FeatureValueType.NUMBER ||
                  f.valueType == FeatureValueType.STRING)) ||
          ((tab == TabsState.CONFIGURATIONS) &&
              (f.valueType == FeatureValueType.JSON));
    }).toList();
  }

  void swapTab(TabsState tab) {
    _fixFeaturesForTabs(tab);
    _stateSource.add(tab);
  }

  void hideEnvironment(String envId) {
    if (featureStatus.sortedByNameEnvironmentIds.contains(envId)) {
      if (shownEnvironments.contains(envId)) {
        featureStatusBloc.removeShownEnvironment(envId);
      } else {
        featureStatusBloc.addShownEnvironment(envId);
      }
    }
  }

  List<EnvironmentFeatureValues> get sortedEnvironmentsThatAreShowing {
    return featureStatus.sortedByNameEnvironmentIds
        .where((id) => shownEnvironments.contains(id))
        .map((id) => featureStatus.applicationEnvironments[id])
        .toList();
  }

  @override
  void dispose() {
    // clean up any outstanding value blocs
    featureValueBlocs.values.forEach((element) => element.dispose());
    _featureStream.cancel();
    _newFeatureStream.cancel();
    _shownEnvironmentsStream.cancel();
  }

  void hideOrShowFeature(Feature feature) {
    final val = _currentlyEditingFeatureKeys;

    if (!val.contains(feature.key)) {
      _createFeatureValueBlocForFeature(feature);
      val.add(feature.key);
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
    var values = featureStatus.applicationFeatureValues.environments
        .where((env) => shownEnvironments.contains(env.environmentId))
        .map((env) => env.features
            .firstWhere((fv) => fv.key == feature.key, orElse: () => null))
        .where((fv) => fv != null)
        .toList();

    featureValueBlocs[feature.key] = FeatureValuesBloc(
        applicationId,
        feature,
        mrClient,
        values,
        featureStatusBloc,
        featureStatus.applicationFeatureValues);
  }

  Feature findFeature(String key, String environmentId) {
    return _allFeaturesByKey[key];
  }

  void _checkForFeaturesWeWereEditingThatHaveNowGone() {
    final removeMissing = _currentlyEditingFeatureKeys;

    removeMissing.where((key) {
      final remove = _allFeaturesByKey[key] == null;
      if (remove) {
        // get rid of the block
        featureValueBlocs[key].dispose();
        featureValueBlocs.remove(key);
      }
      return remove;
    });

    // now make sure all features are there as
    // when we create one we can add it to the list and it not have a bloc
    removeMissing.forEach((key) {
      if (featureValueBlocs[key] == null) {
        _createFeatureValueBlocForFeature(_allFeaturesByKey[key]);
      }
    });

    // trigger a rebuild
    _featureCurrentlyEditingSource.add(removeMissing);
  }
}
