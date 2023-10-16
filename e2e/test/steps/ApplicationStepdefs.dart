import 'package:collection/collection.dart' show IterableExtension;
import 'package:e2e_tests/shared.dart';
import 'package:e2e_tests/user_common.dart';
import 'package:mrapi/api.dart' as api;
import 'package:mrapi/api.dart';
import 'package:ogurets/ogurets.dart';
import 'package:openapi_dart_common/openapi.dart';

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

    var app = await userCommon.findExactApplication(name, p!.id);
    if (app == null) {
      app = await userCommon.applicationService.createApplication(
          p.id!, api.Application(name: name, description: desc));
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
        .findApplications(p!.id!, filter: name);
    assert(apps.firstWhereOrNull((a) => a.name == name) != null,
        'could not find app $name');
  }

  @Then(r'I am able to find application called {string}')
  void iAmAbleToFindApplicationCalled(String name) async {
    var apps = await userCommon.applicationService
        .findApplications(shared.portfolio.id!, filter: name);
    assert(apps.firstWhereOrNull((a) => a.name == name) != null,
        'could not find app $name');
  }

  @And(
      r'I am able to update the application with the name {string} to the name {string} with the description {string} in the portfolio {string}')
  void
      iAmAbleToUpdateTheApplicationWithTheNameToTheNameWithTheDescriptionInThePortfolio(
          String name, String newName, String newDesc, String portfolio) async {
    var p = await userCommon.findExactPortfolio(portfolio);
    assert(p != null, 'Could not find portfolio');

    var app = await userCommon.findExactApplication(name, p!.id);
    assert(app != null, 'Unable to find app $name');

    await userCommon.applicationService.updateApplication(
        app!.id!,
        api.Application(
            name: newName, description: newDesc, version: app.version));
  }

  @And(
      r'I am able to update the application with the name {string} to the name {string} with the description {string}')
  void iAmAbleToUpdateTheApplicationWithTheNameToTheNameWithTheDescription(
      String name, String newName, String newDesc) async {
    var app = await userCommon.findExactApplication(name, shared.portfolio.id);
    assert(app != null, 'Unable to find app $name');

    await userCommon.applicationService.updateApplication(
        app!.id!,
        api.Application(
            name: newName, description: newDesc, id: app.id!, version: app.version));
  }

  @And(r'I delete the application called {string} in the portfolio {string}')
  void iDeleteTheApplicationCalledInThePortfolio(
      String name, String portfolio) async {
    var p = await userCommon.findExactPortfolio(portfolio);
    assert(p != null, 'Could not find portfolio');

    var app = await userCommon.findExactApplication(name, p!.id);
    assert(app != null, 'Unable to find app $name');

    assert(await userCommon.applicationService.deleteApplication(app!.id!),
        'Unable to delete app $name');
  }

  @And(r'I delete the application called {string}')
  void iDeleteTheApplicationCalled(String name) async {
    var app = await userCommon.findExactApplication(name, shared.portfolio.id);
    assert(app != null, 'Unable to find app $name');

    assert(await userCommon.applicationService.deleteApplication(app!.id!),
        'Unable to delete app $name');
  }

  @Then(
      r'I am not able to find application called {string} in the portfolio {string}')
  void iAmNotAbleToFindApplicationCalledInThePortfolio(
      String name, String portfolio) async {
    var p = await userCommon.findExactPortfolio(portfolio);
    assert(p != null, 'Could not find portfolio');

    var app = await userCommon.findExactApplication(name, p!.id);
    assert(app == null, 'Still able to find app $name');
  }

  @Then(r'I am not able to find application called {string}')
  void iAmNotAbleToFindApplicationCalled(String name) async {
    var app = await userCommon.findExactApplication(name, shared.portfolio.id);
    assert(app == null, 'Still able to find app $name');
  }

  @And(r'The application has environments')
  void theApplicationHasEnvironments() async {
    var app = shared.application;
    var currentApp = await userCommon.applicationService
        .getApplication(app.id!, includeEnvironments: true);

    assert(
        currentApp.environments.isNotEmpty, 'Application has no environments');

    var matchedApps = await userCommon.applicationService.findApplications(
        app.portfolioId!,
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

    final result = await userCommon.featureService.createFeaturesForApplication(
        shared.application.id!,
        api.Feature(name: name)
          ..key = key
          ..alias = alias
          ..link = link
          ..valueType = fvt);

    assert(result.firstWhereOrNull((f) => f.key == key) != null,
        'Was not able to create feature');
  }

  @And(r'I ensure that the feature with the key {string} has been removed')
  void iEnsureThatTheFeatureWithTheKeyHasBeenRemoved(String key) async {
    // delete but without being punishing

    try {
      await userCommon.featureService
          .deleteFeatureForApplication(shared.application.id!, key);
    } catch (e) {}
  }

  @When(r'I delete the feature with the key {string}')
  void iDeleteTheFeatureWithTheKey(String key) async {
    // this will throw a 404 if not found
    await userCommon.featureService
        .deleteFeatureForApplication(shared.application.id!, key);
  }

  @And(r'I can find the feature with a key {string}')
  void iCanFindTheFeatureWithAName(String key) async {
    final features = await userCommon.featureService
        .getAllFeaturesForApplication(shared.application.id!);

    assert(features.firstWhereOrNull((f) => f.key == key) != null,
        'Cannot find feature with key $key');
  }

  @And(r'I cannot find the feature with a key {string}')
  void iCannotFindTheFeatureWithAName(String key) async {
    final features = await userCommon.featureService
        .getAllFeaturesForApplication(shared.application.id!);

    assert(features.firstWhereOrNull((f) => f.key == key) == null,
        'Found feature of name $key and should not have.');
  }

  @And(r'I rename the feature with the key {string} to {string}')
  void iRenameTheFeatureWithTheKeyTo(String originalKey, String newKey) async {
    final features = await userCommon.featureService
        .getAllFeaturesForApplication(shared.application.id!);
    final oldFeature = features.firstWhereOrNull((f) => f.key == originalKey);
    assert(
        oldFeature != null, 'Could not find feature to rename: $originalKey.');
    oldFeature!.key = newKey;
    await userCommon.featureService.updateFeatureForApplication(
        shared.application.id!, originalKey, oldFeature);
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
    final group =
        await userCommon.findExactGroup(groupName, shared.portfolio.id);
    assert(group != null, 'Unable to find group');
    var agr = group!.applicationRoles
        .firstWhereOrNull((agr) => agr.applicationId == shared.application.id);
    if (agr == null) {
      agr = api.ApplicationGroupRole(
          applicationId: shared.application.id!, groupId: group.id!, roles: []);
      group.applicationRoles.add(agr);
    }

    api.ApplicationRoleType? desiredRole =
        api.ApplicationRoleTypeExtension.fromJson(roleName);

    if (desiredRole != null && !agr.roles.any((role) => role == desiredRole)) {
      agr.roles.add(desiredRole);
    }

    await userCommon.groupService
        .updateGroup(group.id!, group, updateApplicationGroupRoles: true);
  }

  @And(
      r'I confirm I have the ability to edit features in the current application')
  void iConfirmIHaveTheAbilityToEditFeaturesInTheCurrentApplication() async {
    final me = await userCommon.personService
        .getPerson("self", includeAcls: true, includeGroups: true);

    final theyDo = me.groups.any((gp) => gp.applicationRoles.any((ar) =>
    (ar.roles.contains(api.ApplicationRoleType.EDIT) || ar.roles.contains(api.ApplicationRoleType.EDIT_AND_DELETE) ) &&
        ar.applicationId == shared.application.id));

    assert(theyDo,
        "User should have a role for an application to edit that feature and they don't");
  }

  @Then(
      r'I can get all feature values for this person with a single environment and READ, EDIT, LOCK, UNLOCK permissions')
  void iCanGetAllFeatureValuesForThisPerson() async {
    api.ApplicationFeatureValues afv = await userCommon.featureService
        .findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
            shared.application.id!);
    final env = afv.environments.firstWhereOrNull(
        (element) => element.environmentId == shared.environment.id);
    assert(env != null, 'shared environment id not included!');

    for (var roleType in [
      RoleType.READ,
      RoleType.CHANGE_VALUE,
      RoleType.LOCK,
      RoleType.UNLOCK
    ]) {
      assert(env!.roles.contains(roleType),
          'Role $roleType is not in list ${afv.environments[0].roles}');
    }
  }

  @Given(r'I create an application with the name {string}')
  void iCreateAnApplicationWithTheName(String appName) async {
    try {
      shared.application = await userCommon.applicationService
          .createApplication(shared.portfolio.id!,
              api.Application(name: appName, description: appName),
              includeEnvironments: true);

      if (shared.application.environments.isNotEmpty) {
        shared.environment = shared.application.environments[0];
      }
    } catch (e) {
      if (e is ApiException) {
        if (e.code == 409) {
          shared.application = (await userCommon.findExactApplication(
              appName, shared.portfolio.id))!;
          return;
        }
      }

      throw e;
    }
  }

  @And(r'I can list applications')
  void iCanListApplications() async {
    // we will get a 401 if we aren't allowed to
    await userCommon.applicationService.findApplications(shared.portfolio.id!);
  }

  @And(r'I cannot list the applications')
  void iCannotListApplications() async {
    // we will get a 401 if we aren't allowed to
    var failed = true;

    try {
      await userCommon.applicationService.findApplications(
          shared.portfolio.id!);
    } catch (e) {
      failed = false;
    }

    assert(!failed, 'We were able to list applications and should not have been');
  }
}
