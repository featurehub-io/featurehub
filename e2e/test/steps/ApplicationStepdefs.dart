import 'package:app_singleapp/shared.dart';
import 'package:app_singleapp/user_common.dart';
import 'package:mrapi/api.dart' as api;
import 'package:mrapi/api.dart';
import 'package:ogurets/ogurets.dart';

class ApplicationStepdefs {
  final UserCommon userCommon;
  final Shared shared;

  ApplicationStepdefs(this.userCommon, this.shared);

  @When(
      r'I ensure an application with the name {string} with description {string} in the portfolio {string} exists')
  void iCreateAnApplicationWithTheNameWithDescriptionInThePortfolio(
      String name, String desc, String portfolio) async {
    var p = await userCommon.findExactPortfolio(portfolio);
    assert(p != null, 'Could not find portfolio');

    var app = await userCommon.findExactApplication(name, p.id);
    if (app == null) {
      app = await userCommon.applicationService.createApplication(
          p.id,
          api.Application()
            ..name = name
            ..description = desc);

      assert(app != null, 'Failed to create application');
    }

    shared.application = app;
  }

  @Then(
      r'I am able to find application called {string} in the portfolio {string}')
  void iAmAbleToFindApplicationCalledInThePortfolio(
      String name, String portfolio) async {
    var p = await userCommon.findExactPortfolio(portfolio);
    assert(p != null, 'Could not find portfolio');

    var apps = await userCommon.applicationService
        .findApplications(p.id, filter: name);
    assert(apps != null, 'failed to return any apps at all');
    assert(apps.firstWhere((a) => a.name == name, orElse: () => null) != null,
        'could not find app $name');
  }

  @And(
      r'I am able to update the application with the name {string} to the name {string} with the description {string} in the portfolio {string}')
  void
      iAmAbleToUpdateTheApplicationWithTheNameToTheNameWithTheDescriptionInThePortfolio(
          String name, String newName, String newDesc, String portfolio) async {
    var p = await userCommon.findExactPortfolio(portfolio);
    assert(p != null, 'Could not find portfolio');

    var app = await userCommon.findExactApplication(name, p.id);
    assert(app != null, 'Unable to find app $name');

    var newApp = await userCommon.applicationService.updateApplication(
        app.id,
        api.Application()
          ..version = app.version
          ..name = newName
          ..description = newDesc);

    assert(newApp != null, 'could not update app $name');
  }

  @And(r'I delete the application called {string} in the portfolio {string}')
  void iDeleteTheApplicationCalledInThePortfolio(
      String name, String portfolio) async {
    var p = await userCommon.findExactPortfolio(portfolio);
    assert(p != null, 'Could not find portfolio');

    var app = await userCommon.findExactApplication(name, p.id);
    assert(app != null, 'Unable to find app $name');

    assert(await userCommon.applicationService.deleteApplication(app.id),
        'Unable to delete app $name');
  }

  @Then(
      r'I am not able to find application called {string} in the portfolio {string}')
  void iAmNotAbleToFindApplicationCalledInThePortfolio(
      String name, String portfolio) async {
    var p = await userCommon.findExactPortfolio(portfolio);
    assert(p != null, 'Could not find portfolio');

    var app = await userCommon.findExactApplication(name, p.id);
    assert(app == null, 'Still able to find app $name');
  }

  @And(r'The application has environments')
  void theApplicationHasEnvironments() async {
    var app = shared.application;
    assert(app != null, 'You must create the application first');

    var currentApp = await userCommon.applicationService
        .getApplication(app.id, includeEnvironments: true);

    assert(
        currentApp.environments.isNotEmpty, 'Application has no envronments');

    var matchedApps = await userCommon.applicationService.findApplications(
        app.portfolioId,
        includeEnvironments: true,
        filter: app.name);

    assert(matchedApps.length == 1, 'Could not find match for application.');

    currentApp = matchedApps[0];

    assert(
        currentApp.environments.isNotEmpty, 'Application has no envronments');
  }

  @When(
      r'I create the feature with a key {string} and alias {string} and name {string} and link {string} and type {string}')
  void iCreateTheFeatureWithAKeyAndAliasAndName(String key, String alias,
      String name, String link, String valueType) async {
    FeatureValueType fvt = mapFeatureValueType(valueType);

    assert(shared.application != null,
        'You must have a step to creates or finds an application before this step.');

    final result = await userCommon.featureService.createFeaturesForApplication(
        shared.application.id,
        api.Feature()
          ..key = key
          ..name = name
          ..alias = alias
          ..link = link
          ..valueType = fvt);

    assert(result.firstWhere((f) => f.key == key, orElse: () => null) != null,
        'Was not able to create feature');
  }

  @And(r'I ensure that the feature with the key {string} has been removed')
  void iEnsureThatTheFeatureWithTheKeyHasBeenRemoved(String key) async {
    // delete but without being punishing

    assert(shared.application != null,
        'You must have a step to creates or finds an application before this step.');

    try {
      await userCommon.featureService
          .deleteFeatureForApplication(shared.application.id, key);
    } catch (e) {}
  }

  @When(r'I delete the feature with the key {string}')
  void iDeleteTheFeatureWithTheKey(String key) async {
    assert(shared.application != null,
        'You must have a step to creates or finds an application before this step.');

    // this will throw a 404 if not found
    await userCommon.featureService
        .deleteFeatureForApplication(shared.application.id, key);
  }

  @And(r'I can find the feature with a key {string}')
  void iCanFindTheFeatureWithAName(String key) async {
    assert(shared.application != null,
        'You must have a step to creates or finds an application before this step.');

    final features = await userCommon.featureService
        .getAllFeaturesForApplication(shared.application.id);

    assert(features.firstWhere((f) => f.key == key, orElse: () => null) != null,
        'Cannot find feature with key $key');
  }

  @And(r'I cannot find the feature with a key {string}')
  void iCannotFindTheFeatureWithAName(String key) async {
    assert(shared.application != null,
        'You must have a step to creates or finds an application before this step.');

    final features = await userCommon.featureService
        .getAllFeaturesForApplication(shared.application.id);

    assert(features.firstWhere((f) => f.key == key, orElse: () => null) == null,
        'Found feature of name $key and should not have.');
  }

  @And(r'I rename the feature with the key {string} to {string}')
  void iRenameTheFeatureWithTheKeyTo(String originalKey, String newKey) async {
    assert(shared.application != null,
        'You must have a step to creates or finds an application before this step.');
    final features = await userCommon.featureService
        .getAllFeaturesForApplication(shared.application.id);
    final oldFeature =
        features.firstWhere((f) => f.key == originalKey, orElse: () => null);
    assert(
        oldFeature != null, 'Could not find feature to rename: $originalKey.');
    oldFeature.key = newKey;
    await userCommon.featureService.updateFeatureForApplication(
        shared.application.id, originalKey, oldFeature);
  }

  mapFeatureValueType(String valueType) {
    switch (valueType) {
      case "boolean":
        return FeatureValueType.BOOLEAN;
      case "string":
        return FeatureValueType.STRING;
      case "json":
        return FeatureValueType.JSON;
      case "number":
        return FeatureValueType.NUMBER;
    }
  }

  @And(r'I add the application role {string} to the group called {string}')
  void iAddTheApplicationRoleToTheGroupCalled(
      String roleName, String groupName) async {
    assert(shared.portfolio != null, 'you must have an active portfolio');
    assert(shared.application != null, 'you must have an active application');
    final group =
        await userCommon.findExactGroup(groupName, shared.portfolio.id);
    assert(group != null, 'Unable to find group');
    var agr = group.applicationRoles.firstWhere(
        (agr) => agr.applicationId == shared.application.id,
        orElse: () => null);
    if (agr == null) {
      agr = api.ApplicationGroupRole()
        ..applicationId = shared.application.id
        ..groupId = group.id;
      group.applicationRoles.add(agr);
    }

    api.ApplicationRoleType desiredRole =
        api.ApplicationRoleTypeTypeTransformer.fromJson(roleName);

    if (!agr.roles.any((role) => role == desiredRole)) {
      agr.roles.add(desiredRole);
    }

    await userCommon.groupService
        .updateGroup(group.id, group, updateApplicationGroupRoles: true);
  }

  @And(
      r'I confirm I have the ability to edit features in the current application')
  void iConfirmIHaveTheAbilityToEditFeaturesInTheCurrentApplication() async {
    assert(shared.portfolio != null, 'you must have an active portfolio');
    assert(shared.application != null, 'you must have an active application');

    final me = await userCommon.personService
        .getPerson("self", includeAcls: true, includeGroups: true);

    final theyDo = me.groups.any((gp) => gp.applicationRoles.any((ar) =>
        ar.roles.contains(api.ApplicationRoleType.FEATURE_EDIT) &&
        ar.applicationId == shared.application.id));

    assert(theyDo,
        "User should have a role for an application to edit that feature and they don't");
  }

  @Then(
      r'I can get all feature values for this person with a single environment and READ, EDIT, LOCK, UNLOCK permissions')
  void iCanGetAllFeatureValuesForThisPerson() async {
    assert(shared.environment != null,
        'we dont know what environment is being used');
    api.ApplicationFeatureValues afv = await userCommon.featureService
        .findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
            shared.application.id);
    assert(afv != null, 'Couldnt call all feature values for an application');
    final env = afv.environments.firstWhere(
        (element) => element.environmentId == shared.environment.id,
        orElse: () => null);
    assert(env != null, 'shared environment id not included!');

    for (var roleType in [
      RoleType.READ,
      RoleType.CHANGE_VALUE,
      RoleType.LOCK,
      RoleType.UNLOCK
    ]) {
      assert(env.roles.contains(roleType),
          'Role $roleType is not in list ${afv.environments[0].roles}');
    }
  }

  @Given(r'I create an application with the name {string}')
  void iCreateAnApplicationWithTheName(String appName) async {
    assert(shared.portfolio != null, 'must have set a portfolio');

    shared.application = await userCommon.applicationService.createApplication(
        shared.portfolio.id,
        api.Application()
          ..name = appName
          ..description = appName);
  }
}
