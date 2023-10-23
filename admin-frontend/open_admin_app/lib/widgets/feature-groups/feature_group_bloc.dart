import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/fhos_logger.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_groups_bloc.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/edit_strategy_interface.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:rxdart/rxdart.dart';

class FeatureGroupBloc implements Bloc, EditStrategyBloc<GroupRolloutStrategy> {
  final FeatureGroupsBloc featureGroupsBloc;
  final FeatureGroupListGroup featureGroupListGroup;
  late ApplicationRolloutStrategyServiceApi _appStrategyServiceApi;

  FeatureGroupBloc(this.featureGroupsBloc, this.featureGroupListGroup) {
    _appStrategyServiceApi = ApplicationRolloutStrategyServiceApi(
        featureGroupsBloc.mrClient.apiClient);
    _getFeatureGroup();
    _getAllFeaturesPerEnvironment();
  }

  final _featureGroupStream = BehaviorSubject<FeatureGroup>();

  BehaviorSubject<FeatureGroup> get featureGroupStream => _featureGroupStream;

  final _availableFeaturesStream = BehaviorSubject<List<FeatureGroupFeature>>();

  BehaviorSubject<List<FeatureGroupFeature>> get availableFeaturesStream =>
      _availableFeaturesStream;

  BehaviorSubject<List<FeatureGroupFeature>> get groupFeaturesStream =>
      _trackingUpdatesGroupFeaturesStream;

  final _trackingUpdatesGroupFeaturesStream =
      BehaviorSubject<List<FeatureGroupFeature>>.seeded([]);

  final _trackingUpdatesGroupStrategiesStream =
      BehaviorSubject<List<GroupRolloutStrategy>>.seeded([]);

  final _strategySource = BehaviorSubject<GroupRolloutStrategy?>();

  BehaviorSubject<GroupRolloutStrategy?> get strategyStream => _strategySource;

  final _isGroupUpdatedSource = BehaviorSubject<bool>.seeded(false);
  BehaviorSubject<bool> get isGroupUpdatedStream => _isGroupUpdatedSource;

  String? _selectedFeatureToAdd;

  set selectedFeatureToAdd(String? currentFeatureToAdd) {
    _selectedFeatureToAdd = currentFeatureToAdd;
  }

  @override
  void dispose() {
    _availableFeaturesStream.close();
    _strategySource.close();
    _trackingUpdatesGroupFeaturesStream.close();
    _trackingUpdatesGroupStrategiesStream.close();
  }

  Future<void> _getFeatureGroup() async {
    FeatureGroup fg = await featureGroupsBloc.featureGroupServiceApi
        .getFeatureGroup(
            featureGroupsBloc.mrClient.currentAid!, featureGroupListGroup.id);
    _featureGroupStream.add(fg);
    if (fg.strategies?.isNotEmpty == true) {
      _strategySource.add(fg.strategies![0]);
    }
    _trackingUpdatesGroupFeaturesStream.add(fg.features);
    _trackingUpdatesGroupStrategiesStream.add(fg.strategies ?? []);
  }

  Future<void> _getAllFeaturesPerEnvironment() async {
    // this gets all features available in the environment, and returns them as a List of FeatureGroupFeature so
    // we can list in the drop-down all available features to be added to a group
    var feat = await featureGroupsBloc.featureGroupServiceApi
        .getFeatureGroupFeatures(featureGroupsBloc.mrClient.currentAid!,
            featureGroupListGroup.environmentId);
    _availableFeaturesStream.add(feat);
  }

  @override
  void addStrategy(EditingRolloutStrategy strategy) {
    fhosLogger.fine("adding new strategy $strategy to stream");
    final fgStrategy = strategy.toGroupRolloutStrategy()!;

    _strategySource.add(fgStrategy);
    List<GroupRolloutStrategy> strategyList =
        _trackingUpdatesGroupStrategiesStream.value;
    strategyList.clear(); // we can only have 1
    strategyList.add(fgStrategy);
    _trackingUpdatesGroupStrategiesStream.add(strategyList);
    _isGroupUpdatedSource.add(true);
  }

  @override
  void updateStrategy() {
    GroupRolloutStrategy strategy = _strategySource.value!;
    List<GroupRolloutStrategy> strategyList =
        []; // create new list is ok here, as we only have one strategy
    strategyList.add(strategy);
    _trackingUpdatesGroupStrategiesStream.add(strategyList);
    _isGroupUpdatedSource.add(true);
  }

  void addFeatureToGroup() {
    if (_selectedFeatureToAdd != null) {
      final latestFeatureGroupFeatures =
          _trackingUpdatesGroupFeaturesStream.value;
      List<FeatureGroupFeature> features = _availableFeaturesStream.value;
      FeatureGroupFeature currentFeatureGF = features
          .firstWhere((feature) => feature.id == _selectedFeatureToAdd!);
      if (!latestFeatureGroupFeatures
          .any((feature) => feature.id == currentFeatureGF.id)) {
        fhosLogger.fine("adding feature ${currentFeatureGF} to tracking");
        latestFeatureGroupFeatures.add(currentFeatureGF);
        _trackingUpdatesGroupFeaturesStream.add(latestFeatureGroupFeatures);
      }
    }
    _isGroupUpdatedSource.add(true);
  }

  void removeFeatureFromGroup(FeatureGroupFeature groupFeature) {
    var latestFeatureGroupFeatures = _trackingUpdatesGroupFeaturesStream.value;
    latestFeatureGroupFeatures.removeWhere((gf) => gf.id == groupFeature.id);
    _trackingUpdatesGroupFeaturesStream.add(latestFeatureGroupFeatures);
    _isGroupUpdatedSource.add(true);
  }

  Future<void> saveFeatureGroupUpdates() async {
    final features = _trackingUpdatesGroupFeaturesStream.hasValue
        ? _trackingUpdatesGroupFeaturesStream.value
            .map((e) => convertToFeatureUpdate(e))
            .toList()
        : null;

    final strategies = _trackingUpdatesGroupStrategiesStream.hasValue
        ? _trackingUpdatesGroupStrategiesStream.value
        : null;

    await featureGroupsBloc.updateFeatureGroup(featureGroupListGroup,
        features: features, strategies: strategies);
  }

  FeatureGroupUpdateFeature convertToFeatureUpdate(
      FeatureGroupFeature feature) {
    return FeatureGroupUpdateFeature(id: feature.id, value: feature.value);
  }

  void setFeatureValue(dynamic newValue, FeatureGroupFeature feature) {
    List<FeatureGroupFeature> features =
        _trackingUpdatesGroupFeaturesStream.value;
    int index = features.indexWhere((f) => feature.id == f.id);
    features[index].value = newValue;
    _trackingUpdatesGroupFeaturesStream.add(features);
    _isGroupUpdatedSource.add(true);
  }

  @override
  void removeStrategy(strategy) {
    final strategies = _trackingUpdatesGroupStrategiesStream.value;
    strategies.removeWhere((e) =>
        e.name ==
        strategy
            .name); // ideally need an id, but because we only have one strategy at the moment, it is ok
    _strategySource.add(null);
    _trackingUpdatesGroupStrategiesStream.add(strategies);
    _isGroupUpdatedSource.add(true);
  }

  @override
  Future<RolloutStrategyValidationResponse> validationCheck(strategy) async {
    var rs = RolloutStrategy(
        id: strategy.id,
        name: strategy.name,
        percentage: strategy.percentage,
        attributes: strategy.attributes);

    List<RolloutStrategy> strategies = [];
    strategies.add(rs);

    return _appStrategyServiceApi.validate(
        featureGroupsBloc.mrClient.currentAid!,
        RolloutStrategyValidationRequest(
          customStrategies: strategies,
          sharedStrategies: <RolloutStrategyInstance>[],
        ));
  }

  @override
  get feature {}
}
