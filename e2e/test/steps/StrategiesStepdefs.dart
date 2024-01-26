import 'package:collection/collection.dart';
import 'package:e2e_tests/shared.dart';
import 'package:e2e_tests/user_common.dart';
import 'package:mrapi/api.dart';
import 'package:ogurets/ogurets.dart';

class StrategiesStepdefs {
  final UserCommon userCommon;
  final Shared shared;

  StrategiesStepdefs(this.userCommon, this.shared);

  @And(r'I create shared rollout strategies')
  void iSetTheRolloutStrategyToPercentage(GherkinTable table) async {
    assert(shared.environment.applicationId == shared.application.id,
        'environment is not in application');
    assert(shared.feature != null, 'must know what the feature is');

    final existing = (await userCommon.rolloutStrategyService
            .listApplicationStrategies(shared.application.id))
        .items;

    for (var g in table) {
      var strategy =
          existing.firstWhereOrNull((s) => s.name.toLowerCase() == g['name']);

      var percentage = g['percentage'] != null
          ? (double.parse(g['percentage']) * 10000).round()
          : null;

      if (strategy == null) {
        await userCommon.rolloutStrategyService.createApplicationStrategy(
            shared.application.id,
            CreateApplicationRolloutStrategy(
                name: g['name'], percentage: percentage));
      } else {
        strategy.percentage = percentage;

        await userCommon.rolloutStrategyService.updateApplicationStrategy(
            shared.application.id,
            strategy.id,
            UpdateApplicationRolloutStrategy(percentage: percentage));
      }
    }
  }

  @And(r'I apply the rollout strategies to the current feature value')
  void iApplyRolloutStrategiesToFeatureValue(GherkinTable table) async {
    assert(shared.environment.applicationId == shared.application.id,
        'environment is not in application');
    assert(shared.feature != null, 'must know what the feature is');

    FeatureValue fv;

    try {
      fv = await userCommon.environmentFeatureServiceApi
          .getFeatureForEnvironment(
              shared.environment.id, shared.feature!.key);
    } catch (e) {
      fv = FeatureValue(
        key: shared.feature!.key,
        rolloutStrategyInstances: [],
        locked: false,
      );
    }

    // now find the shared strategies
    for (var g in table) {
      String name = g["name"];

      final strategy = await userCommon.rolloutStrategyService
          .getApplicationStrategy(shared.application.id, name);

      var rsi = fv.rolloutStrategyInstances!.firstWhere(
          (element) => element.strategyId == strategy.id, orElse: () {
        final r = RolloutStrategyInstance(
          strategyId: strategy.id, name: name
        );
        fv.rolloutStrategyInstances!.add(r);
        return r;
      });

      rsi.value = g["value"];
    }

    if (fv.id != null) {
      shared.featureValue = (await userCommon.environmentFeatureServiceApi
          .updateAllFeaturesForEnvironment(shared.environment.id, [fv]))[0];
    } else {
      shared.featureValue = await userCommon.environmentFeatureServiceApi
          .createFeatureForEnvironment(
              shared.environment.id, shared.feature!.key, fv);
    }
  }

  @And(r'I confirm on getting the feature it has the same data as set')
  void iConfirmOnGettingTheFeatureItHasTheSameDataAsSet() async {
    assert(shared.feature != null, 'must know what the feature is');
    assert(shared.featureValue != null,
        'must have a stored feature value to compare against');

    final fv =
        await userCommon.environmentFeatureServiceApi.getFeatureForEnvironment(
      shared.environment.id,
      shared.feature!.key,
    );

    print(
        "fv is ${fv.rolloutStrategyInstances}\n stored is ${shared.featureValue!.rolloutStrategyInstances}");

    assert(
        ListEquality().equals(
            fv.rolloutStrategyInstances!.sortedBy((element) => element.name),
            shared.featureValue!.rolloutStrategyInstances!
                .sortedBy((element) => element.name)),
        'not equal');
  }

  @And(r'I create custom rollout strategies')
  void iCreateCustomRolloutStrategies(GherkinTable table) async {
    assert(shared.feature != null, 'must know what the feature is');
    assert(shared.featureValue != null,
        'must have a stored feature value to compare against');

    final fv =
        await userCommon.environmentFeatureServiceApi.getFeatureForEnvironment(
      shared.environment.id,
      shared.feature!.key,
    );

    fv.rolloutStrategies = [];
    _updateStrategiesFromTable(table, fv.rolloutStrategies!);

    shared.featureValue = (await userCommon.environmentFeatureServiceApi
        .updateAllFeaturesForEnvironment(shared.environment.id, [fv]))[0];
  }

  void _updateStrategiesFromTable(
      GherkinTable table, List<RolloutStrategy> rolloutStrategies) {
    for (var g in table) {
      final rs = rolloutStrategies.firstWhere((rs) => rs.name == g['name'],
          orElse: () {
        final r = RolloutStrategy(
          name: g['name'],
        );
        rolloutStrategies.add(r);
        return r;
      });

      switch (shared.feature!.valueType) {
        case FeatureValueType.BOOLEAN:
          rs.value = 'true' == g['value'];
          break;
        case FeatureValueType.STRING:
          rs.value = g['value'];
          break;
        case FeatureValueType.NUMBER:
          rs.value = double.parse(g['value']);
          break;
        case FeatureValueType.JSON:
          rs.value = g['value'];
          break;
      }

      rs.percentage = (double.parse(g['percentage']) * 10000.0).toInt();
      if (g['percentageAttributes'] != null) {
        rs.percentageAttributes = g['percentageAttributes']
            .toString()
            .split(',')
            .map((e) => e.trim())
            .where((e) => e.isNotEmpty)
            .toList();
      }
    }
  }

  @And(
      r'I confirm on getting the feature value has the custom rollout strategies set')
  void
      iConfirmOnGettingTheFeatureValueHasTheCustomRolloutStrategiesSet() async {
    assert(shared.feature != null, 'must know what the feature is');
    assert(shared.featureValue != null,
        'must have a stored feature value to compare against');

    final fv =
        await userCommon.environmentFeatureServiceApi.getFeatureForEnvironment(
      shared.environment.id,
      shared.feature!.key,
    );

    print(
        "fv is ${fv.rolloutStrategies}\n stored is ${shared.featureValue!.rolloutStrategies}");

    final rs1 = fv.rolloutStrategies;
    final rs2 = shared.featureValue!.rolloutStrategies;
    assert(rs1!.length == rs2!.length);
    for (var count = 0; count < rs2!.length; count++) {
      final r1 = rs1![count];
      final r2 = rs2[count];

      assert(ListEquality()
          .equals(r1.percentageAttributes, r2.percentageAttributes));
      assert(r1.percentage == r2.percentage);
      assert(r1.name == r2.name);
    }
  }
}
