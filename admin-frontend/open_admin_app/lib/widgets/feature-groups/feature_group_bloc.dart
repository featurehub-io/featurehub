import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/feature-groups/feature-groups-bloc.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/edit_strategy_interface.dart';
import 'package:rxdart/rxdart.dart';
import 'package:collection/collection.dart';

class FeatureGroupBloc implements Bloc, EditStrategyBloc<FeatureGroupStrategy> {
  final FeatureGroupsBloc featureGroupsBloc;
  final FeatureGroupListGroup featureGroupListGroup;
  late EnvironmentFeatureServiceApi _featureServiceApi;
  late RolloutStrategyServiceApi _rolloutStrategyServiceApi;

  FeatureGroupBloc(this.featureGroupsBloc, this.featureGroupListGroup) {
    _featureServiceApi =
        EnvironmentFeatureServiceApi(featureGroupsBloc.mrClient.apiClient);
    _rolloutStrategyServiceApi =
        RolloutStrategyServiceApi(featureGroupsBloc.mrClient.apiClient);
    _getFeatureGroup();
    _getFeatures();
  }

  final _featureGroupStream = BehaviorSubject<FeatureGroup>();
  BehaviorSubject<FeatureGroup> get featureGroupStream => _featureGroupStream;

  final _availableFeaturesStream = BehaviorSubject<List<Feature>>();
  BehaviorSubject<List<Feature>> get availableFeaturesStream =>
      _availableFeaturesStream;

  final _availableFeatureValuesStream = BehaviorSubject<List<FeatureValue>>();

  BehaviorSubject<List<FeatureGroupFeature>> get groupFeaturesStream =>
      _trackingUpdatesGroupFeaturesStream;

  final _trackingUpdatesGroupFeaturesStream =
      BehaviorSubject<List<FeatureGroupFeature>>.seeded([]);

  final _trackingUpdatesGroupStrategiesStream =
      BehaviorSubject<List<FeatureGroupStrategy>>.seeded([]);

  final _strategySource = BehaviorSubject<FeatureGroupStrategy?>();
  BehaviorSubject<FeatureGroupStrategy?> get strategyStream => _strategySource;

  String? _selectedFeatureToAdd;

  set selectedFeatureToAdd(String? currentFeatureToAdd) {
    _selectedFeatureToAdd = currentFeatureToAdd;
  }

  @override
  void dispose() {
    _availableFeaturesStream.close();
    _availableFeatureValuesStream.close();
    _strategySource.close();
    _trackingUpdatesGroupFeaturesStream.close();
    _trackingUpdatesGroupStrategiesStream.close();
  }

  Future<void> _getFeatureGroup() async {
    FeatureGroup fg = await featureGroupsBloc.featureGroupServiceApi
        .getFeatureGroup(
            featureGroupsBloc.mrClient.currentAid!, featureGroupListGroup.id);
    _featureGroupStream.add(fg);
    if (fg.strategies.isNotEmpty) {
      _strategySource.add(fg.strategies[0]);
    }
    _trackingUpdatesGroupFeaturesStream.add(fg.features);
    _trackingUpdatesGroupStrategiesStream.add(fg.strategies);
  }

  Future<void> _getFeatures() async {
    var feat = await _featureServiceApi.getFeaturesForEnvironment(
        featureGroupListGroup.environmentId,
        includeFeatures: true);
    _availableFeaturesStream.add(feat.features);
    _availableFeatureValuesStream.add(feat.featureValues);
  }

  @override
  void addStrategy(FeatureGroupStrategy strategy) {
    List<FeatureGroupStrategy> strategyList =
        _trackingUpdatesGroupStrategiesStream.value;
    strategyList.add(strategy);
    _trackingUpdatesGroupStrategiesStream.add(strategyList);
  }

  @override
  void updateStrategy() {
    FeatureGroupStrategy strategy = _strategySource.value!;
    List<FeatureGroupStrategy> strategyList =
        []; // create new list is ok here, as we only have one strategy
    strategyList.add(strategy);
    _trackingUpdatesGroupStrategiesStream.add(strategyList);
  }

  void addFeatureToGroup() {
    var latestFeatureGroupFeatures = _trackingUpdatesGroupFeaturesStream.value;
    if (_selectedFeatureToAdd != null) {
      FeatureGroupFeature currentFeatureGF =
          convertToGroupFeature(_selectedFeatureToAdd!);
      if (!latestFeatureGroupFeatures
          .any((feature) => feature.id == currentFeatureGF.id)) {
        latestFeatureGroupFeatures.add(currentFeatureGF);
        _trackingUpdatesGroupFeaturesStream.add(latestFeatureGroupFeatures);
      }
    }
  }

  void removeFeatureFromGroup(FeatureGroupFeature groupFeature) {
    var latestFeatureGroupFeatures = _trackingUpdatesGroupFeaturesStream.value;
    latestFeatureGroupFeatures.removeWhere((gf) => gf.id == groupFeature.id);
    _trackingUpdatesGroupFeaturesStream.add(latestFeatureGroupFeatures);
  }

  Future<void> saveFeatureGroupUpdates() async {
    List<FeatureGroupUpdateFeature>? features = [];
    List<FeatureGroupStrategy> strategies = [];
    for (FeatureGroupFeature feature
        in _trackingUpdatesGroupFeaturesStream.value) {
      FeatureGroupUpdateFeature featureUpdate = convertToFeatureUpdate(feature);
      features.add(featureUpdate);
    }
    for (FeatureGroupStrategy strategy
        in _trackingUpdatesGroupStrategiesStream.value) {
      strategies.add(strategy);
    }
    await featureGroupsBloc.updateFeatureGroup(featureGroupListGroup,
        features: features, strategies: strategies);
  }

  FeatureGroupUpdateFeature convertToFeatureUpdate(
      FeatureGroupFeature feature) {
    return FeatureGroupUpdateFeature(id: feature.id, value: feature.value);
  }

  FeatureGroupFeature convertToGroupFeature(String id) {
    List<Feature> features = _availableFeaturesStream.value;
    Feature feature = features.firstWhere((feature) => feature.id == id);
    List<FeatureValue> featureValues = _availableFeatureValuesStream.value;
    // feature value could be not present, for example if it is set to null on the main feature dashboard
    FeatureValue? featureValue = featureValues
        .firstWhereOrNull((featureValue) => featureValue.key == feature.key);
    var currentFeatureGF = FeatureGroupFeature(
        id: id,
        name: feature.name,
        key: feature.key!,
        value: feature.valueType == FeatureValueType.BOOLEAN ? false : null,
        type: feature.valueType!,
        locked: featureValue != null
            ? featureValue.locked
            : true); // if feature value is not set on the main dashboard, then display as locked, otherwise there is no value to fall back to
    return currentFeatureGF;
  }

  void setFeatureValue(dynamic newValue, FeatureGroupFeature feature) {
    List<FeatureGroupFeature> features =
        _trackingUpdatesGroupFeaturesStream.value;
    int index = features.indexWhere((f) => feature.id == f.id);
    features[index].value = newValue;
    _trackingUpdatesGroupFeaturesStream.add(features);
  }

  @override
  void addStrategyAttribute() {
    // TODO: implement addStrategyAttribute
  }

  @override
  void ensureStrategiesAreUnique() {}

  @override
  // TODO: implement feature
  get feature => throw UnimplementedError();

  @override
  void removeStrategy(strategy) {
    final strategies = _trackingUpdatesGroupStrategiesStream.value;
    strategies.removeWhere((e) =>
        e.name ==
        strategy
            .name); // ideally need an id, but because we only have one strategy at the moment, it is ok
    _strategySource.add(null);
    _trackingUpdatesGroupStrategiesStream.add(strategies);
  }

  @override
  uniqueStrategyId() {
    throw UnimplementedError();
  }

  @override
  Future<RolloutStrategyValidationResponse> validationCheck(strategy) async {
    // print('validating custom strategies $customStrategies');
    var rs = RolloutStrategy(
        name: strategy.name,
        percentage: strategy.percentage,
        attributes: strategy.attributes);

    List<RolloutStrategy> strategies = [];

    // strategy.id ??= makeStrategyId(existing: strategies);

    strategies.add(rs);

    return _rolloutStrategyServiceApi.validate(
        featureGroupsBloc.mrClient.currentAid!,
        RolloutStrategyValidationRequest(
          customStrategies: strategies,
          sharedStrategies: <RolloutStrategyInstance>[],
        ));
  }
}
