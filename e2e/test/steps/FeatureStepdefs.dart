import 'package:app_singleapp/shared.dart';
import 'package:app_singleapp/user_common.dart';
import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:ogurets/ogurets.dart';

class FeatureStepdefs {
  final UserCommon userCommon;
  final Shared shared;

  FeatureStepdefs(this.userCommon, this.shared);

  @And(
      r'I ensure the boolean feature value is {string} for environment {string} for feature {string}')
  void iEnsureTheBooleanFeatureValueIsForEnvironmentForFeature(
      String value, String envName, String key) async {
    Environment environment =
        await userCommon.findExactEnvironment(envName, shared.application.id);

    assert(environment != null, 'Could not find environment $envName');

    final val = await userCommon.environmentFeatureServiceApi
        .getFeatureForEnvironment(environment.id, key);

    assert(val.valueBoolean.toString() == value);
  }

  @And(
      r'I set the boolean feature value as {string} for environment {string} for feature {string}')
  void iSetTheBooleanFeatureValueAsTrue(
      String value, String env, String featureKey) async {
    Environment environment =
        await userCommon.findExactEnvironment(env, shared.application.id);
    String boolAsString;
    boolAsString = value;
    bool b = boolAsString == 'true';

    FeatureValue featureValue = await userCommon.environmentFeatureServiceApi
        .getFeatureForEnvironment(environment.id, featureKey);

    await userCommon.environmentFeatureServiceApi.updateFeatureForEnvironment(
        environment.id, featureKey, featureValue..valueBoolean = b);
  }

  @And(r'I choose the application {string} in portfolio {string}')
  void iGetAllOfTheFeatureValuesForTheApplicationInPortfolio(
      String app, String portfolio) async {
    final p = await userCommon.findExactPortfolio(portfolio);
    assert(p != null, 'Cannot find portfolio $portfolio');
    final a = await userCommon.findExactApplication(app, p.id);
    assert(a != null, 'Cannot find application $app');

    shared.portfolio = p;
    shared.application = a;
  }

  @When(r'I get the details for feature {string} and set them as follows:')
  void iGetTheDetailsForFeatureAndSetThemAsFollows(
      String featureName, GherkinTable table) async {
    final data = await userCommon.featureService
        .getAllFeatureValuesByApplicationForKey(
            shared.application.id, featureName);
    final environments = await userCommon.environmentService
        .findEnvironments(shared.application.id);
    final features = await userCommon.featureService
        .getAllFeaturesForApplication(shared.application.id);
    final foundFeature =
        features.firstWhere((f) => f.key == featureName, orElse: () => null);
    assert(foundFeature != null, 'no feature called $featureName exits');
    List<FeatureValue> values = [];
    table.forEach((env) {
      final e = environments.firstWhere((e) => e.name == env['envName']);
      final existing = data.firstWhere((fe) => fe.environment.id == e.id);
      if (existing.featureValue == null) {
        existing.featureValue = FeatureValue()
          ..key = featureName
          ..locked = false
          ..environmentId = e.id;
      }
      existing.featureValue.valueBoolean =
          (env['value'] == 'null' ? null : ('true' == env['value']));
      values.add(existing.featureValue);
    });

    await userCommon.featureService.updateAllFeatureValuesByApplicationForKey(
        shared.application.id, featureName, values);

    final newData = await userCommon.featureService
        .getAllFeatureValuesByApplicationForKey(
            shared.application.id, featureName);
    table.forEach((env) {
      final e = environments.firstWhere((e) => e.name == env['envName']);
      final existing = newData.firstWhere((fe) => fe.environment.id == e.id);
      if (existing.featureValue == null) {
        assert(env['value'] == 'null',
            'attempting to set ${env['envName']} to null and failed.');
      } else {
        final shouldBe =
            (env['value'] == 'null' ? null : ('true' == env['value']));
        assert(existing.featureValue.valueBoolean == shouldBe,
            'Expecting $shouldBe but found ${existing.featureValue.valueBoolean}');
      }
    });
  }

  @And(r'I create a feature flag {string}')
  void iCreateAFeature(String feature1) async {
    assert(shared.application != null, 'no application set!');

    await userCommon.featureService.createFeaturesForApplication(
        shared.application.id,
        Feature()
          ..name = feature1
          ..key = feature1
          ..valueType = FeatureValueType.BOOLEAN);
  }

  @And(r'all environments should have {int} feature flags')
  void allEnvironmentsShouldHaveFeatureFlags(int count) async {
    assert(shared.application != null, 'no application set!');

    final app = await userCommon.applicationService
        .getApplication(shared.application.id, includeEnvironments: true);

    for (var e in app.environments) {
      final env = await userCommon.environmentFeatureServiceApi
          .getFeaturesForEnvironment(e.id);

      assert(env.featureValues.length == count);
    }
  }

  @And(r'all feature flags for environment {string} should be {string}')
  void allFeatureFlagsForEnvironmentShouldBe(
      String envName, String flagValueText) async {
    bool flagValue = flagValueText == 'true';
    assert(shared.application != null, 'no application set!');
    final app = await userCommon.applicationService
        .getApplication(shared.application.id, includeEnvironments: true);
    final env =
        app.environments.firstWhere((element) => element.name == envName);
    final efv = await userCommon.environmentFeatureServiceApi
        .getFeaturesForEnvironment(env.id);

    for (var val in efv.featureValues) {
      assert(val.valueBoolean == flagValue,
          'Environment ${env.name} key ${val.key} is ${val.valueBoolean} and not ${flagValue}');
    }
  }

  _changeLock(bool lock, String env, String featureKey) async {
    Environment environment =
        await userCommon.findExactEnvironment(env, shared.application.id);

    FeatureValue featureValue = await userCommon.environmentFeatureServiceApi
        .getFeatureForEnvironment(environment.id, featureKey);

    await userCommon.environmentFeatureServiceApi.updateFeatureForEnvironment(
        environment.id, featureKey, featureValue..locked = lock);
  }

  @And(
      r'^I (lock|unlock) the feature value for environment "(.*)" for feature "(.*)"$')
  void iUnlockTheFeatureValueForEnvironmentForFeature(
      String lockStatus, String env, String featureKey) async {
    await _changeLock(lockStatus == 'lock', env, featureKey);
  }

  @And(r'^I (can|cannot) (lock|unlock|read|change) the feature flag "(.*)"$')
  void iCannotUnlockTheFeatureFlag(
      String allowed, String lock, String featureKey) async {
    final environment = shared.environment;

    try {
      FeatureValue featureValue = await userCommon.environmentFeatureServiceApi
          .getFeatureForEnvironment(environment.id, featureKey);

      if (lock == "read") {
        assert(allowed == "can",
            "was able to read and should not have been able to");

        return;
      }

      if (lock.contains("lock")) {
        await userCommon.environmentFeatureServiceApi
            .updateFeatureForEnvironment(environment.id, featureKey,
                featureValue..locked = (lock == "lock"));
        assert((allowed == "can"),
            "was able to $lock feature flag but shouldn't have been able to");
      } else if (lock.contains("change")) {
        await userCommon.environmentFeatureServiceApi
            .updateFeatureForEnvironment(environment.id, featureKey,
                featureValue..valueBoolean = !featureValue.valueBoolean);
        assert(allowed == "can",
            "we were able to change the feature value but should not have been able to.");
      }
    } catch (e) {
      assert((allowed == "cannot"),
          "was not able to $lock feature flag and should have been able to");
    }
  }

  FeatureValueType textToType(String txt) {
    if (['string', 'text'].contains(txt.toLowerCase())) {
      return FeatureValueType.STRING;
    }

    if (['number'].contains(txt.toLowerCase())) {
      return FeatureValueType.NUMBER;
    }

    if (['json'].contains(txt.toLowerCase())) {
      return FeatureValueType.JSON;
    }

    if (['flag', 'boolean'].contains(txt.toLowerCase())) {
      return FeatureValueType.BOOLEAN;
    }

    return null;
  }

  @And(
      r'^I ensure that the (string|number) feature with the key (.*) exists and has the default value (.*)$')
  void iEnsureThatTheStringFeatureWithTheKeyExistsAndHasTheDefaultValue(
      String type, String featureKey, String defaultValue) async {
    assert(shared.application != null, 'please set application first');
    assert(shared.environment != null, 'please set an environment!');
    assert(shared.environment.applicationId == shared.application.id,
        'environment is not in application');

    final actualType = textToType(type);

    assert(actualType != null, "${type} is not a valid feature type");

    var feature;

    try {
      feature = await userCommon.featureService
          .getFeatureByKey(shared.application.id, featureKey);
      assert(feature.valueType == actualType,
          'feature is not null and of the wrong type, please use another name');
    } catch (e) {
      final features =
          await userCommon.featureService.createFeaturesForApplication(
              shared.application.id,
              new Feature()
                ..valueType = actualType
                ..name = featureKey
                ..key = featureKey);

      feature = features.firstWhere((element) => element.key == featureKey);
    }

    shared.feature = feature;

    final allFeatures = await userCommon.featureService
        .findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
            shared.application.id);

    final efv = allFeatures.environments
        .firstWhere((efv) => efv.environmentId == shared.environment.id);
    var fv = efv.features
        .firstWhere((fv) => fv.key == featureKey, orElse: () => null);

    if (fv == null) {
      fv = FeatureValue()
        ..key = featureKey
        ..environmentId = shared.environment.id;
    }

    switch (actualType) {
      case FeatureValueType.BOOLEAN:
        fv.valueBoolean = "true" == defaultValue;
        break;
      case FeatureValueType.STRING:
        fv.valueString = defaultValue;
        break;
      case FeatureValueType.NUMBER:
        fv.valueNumber = double.parse(defaultValue);
        break;
      case FeatureValueType.JSON:
        fv.valueJson = defaultValue;
        break;
    }

    fv.locked = false;

    await userCommon.featureService.updateAllFeatureValuesByApplicationForKey(
        shared.application.id, featureKey, [fv]);

    shared.featureValue = fv;
  }

  @And(r'I set the rollout strategy {string} to percentage')
  void iSetTheRolloutStrategyToPercentage(
      String name, GherkinTable table) async {
    assert(shared.application != null, 'please set application first');
    assert(shared.environment != null, 'please set an environment!');
    assert(shared.environment.applicationId == shared.application.id,
        'environment is not in application');
    assert(shared.feature != null, 'must know what the feature is');

    final allFeatures = await userCommon.featureService
        .findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
            shared.application.id);

    final efv = allFeatures.environments
        .firstWhere((efv) => efv.environmentId == shared.environment.id);
    final fv = efv.features.firstWhere((fv) => fv.key == shared.feature.key);

    shared.featureValue = fv;

    fv.rolloutStrategyInstances = [];
    for (var g in table)
      fv.rolloutStrategyInstances.add(RolloutStrategyInstance()
        ..name = name
        ..percentage = int.parse(g["percentage"])
        ..valueString = g["value"]);

    await userCommon.featureService.updateAllFeatureValuesByApplicationForKey(
        shared.application.id, fv.key, [fv]);
  }

  @And(r'I confirm on getting the feature it has the same data as set')
  void iConfirmOnGettingTheFeatureItHasTheSameDataAsSet() async {
    assert(shared.application != null, 'please set application first');
    assert(shared.environment != null, 'please set an environment!');
    assert(shared.environment.applicationId == shared.application.id,
        'environment is not in application');
    assert(shared.feature != null, 'must know what the feature is');
    assert(shared.featureValue != null,
        'must have a stored feature value to compare against');

    final allFeatures = await userCommon.featureService
        .findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
            shared.application.id);

    final efv = allFeatures.environments
        .firstWhere((efv) => efv.environmentId == shared.environment.id);
    final fv = efv.features.firstWhere((fv) => fv.key == shared.feature.key);

    print(
        "fv is ${fv.rolloutStrategyInstances}\n stored is ${shared.featureValue.rolloutStrategyInstances}");

    assert(
        new ListEquality().equals(fv.rolloutStrategyInstances,
            shared.featureValue.rolloutStrategyInstances),
        'not equal');
  }
}
