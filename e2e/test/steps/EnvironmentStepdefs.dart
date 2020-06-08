import 'package:app_singleapp/shared.dart';
import 'package:app_singleapp/user_common.dart';
import 'package:mrapi/api.dart';
import 'package:ogurets/ogurets.dart';

class EnvironmentStepdefs {
  final UserCommon common;
  final Shared shared;

  EnvironmentStepdefs(this.common, this.shared);

  Future<Application> _findApplication(
      String portfolioName, String appName) async {
    Portfolio p = await common.findExactPortfolio(portfolioName);
    assert(p != null, 'Cannot find portfolio $portfolioName');

    Application a = await common.findExactApplication(appName, p.id);
    assert(a != null,
        'Cannot find application $appName inside portfolio $portfolioName');

    return a;
  }

  Future<Group> _findGroup(String portfolioName, String groupName,
      bool includeEnvironmentGroupRoles) async {
    Portfolio p = await common.findExactPortfolio(portfolioName);
    assert(p != null, 'Cannot find portfolio $portfolioName');

    Group g = includeEnvironmentGroupRoles
        ? await common.findExactGroupWithPerms(groupName, p.id)
        : await common.findExactGroup(groupName, p.id);

    assert(g != null,
        'Cannot find group $groupName inside portfolio $portfolioName');

    return g;
  }

  @And(
      r'I ensure that an environment {string} with description {string} exists in the app {string} in the portfolio {string}')
  void iEnsureThatAnEnvironmentWithDescriptionExistsInTheAppInThePortfolio(
      String name, String desc, String appName, String portfolioName) async {
    Application application = await _findApplication(portfolioName, appName);

    var exists = await common.findExactEnvironment(name, application.id);
    if (exists == null) {
      exists = await common.environmentService.createEnvironment(
          application.id,
          new Environment()
            ..name = name
            ..description = desc);
    }

    assert(exists != null, 'Could not create environment $name');

    shared.application = application;
    shared.environment = exists;
  }

  @And(r'I can find environment {string} in the application')
  void iCanFindEnvironmentInTheApplication(String name) async {
    Application app = shared.application;
    assert(
        app != null, 'You have missed a step storing the selected application');

    var env = await common.findExactEnvironment(name, app.id);

    assert(env != null, 'Could not find environment $name');
  }

  @And(r'I cannot find environment {string} in the application')
  void iCannotFindEnvironmentInTheApplication(String name) async {
    Application app = shared.application;
    assert(
        app != null, 'You have missed a step storing the selected application');

    var env = await common.findExactEnvironment(name, app.id);

    assert(env == null,
        'Could find environment $name in app ${app.name} and should not be allowed to.');
  }

  @And(
      r'I ensure the permission {string} is added to the group {string} for the env {string} for app {string} for portfolio {string}')
  void iEnsureThePermissionIsAddedToTheGroupForTheEnvForAppForPortfolio(
      String perm,
      String groupName,
      String environmentName,
      String appName,
      String portfolioName) async {
    Application application = await _findApplication(portfolioName, appName);
    Group group = await _findGroup(portfolioName, groupName, true);
    var environment =
        await common.findExactEnvironment(environmentName, application.id);
    EnvironmentGroupRole egr = EnvironmentGroupRole();
    egr.groupId = group.id;
    egr.environmentId = environment.id;
    perm == "READ" ? egr.roles.add(RoleType.READ) : {};
    perm == "EDIT" ? egr.roles.add(RoleType.EDIT) : {};
    perm == "LOCK" ? egr.roles.add(RoleType.LOCK) : {};
    perm == "UNLOCK" ? egr.roles.add(RoleType.UNLOCK) : {};

    group.environmentRoles.add(egr);

    await common.groupService.updateGroup(group.id, group,
        includeGroupRoles: true,
        updateEnvironmentGroupRoles: true,
        updateApplicationGroupRoles: true);

    shared.application = application;
  }

  @And(
      r'I can find perm {string} in the group {string} for the env {string} for app {string} for portfolio {string}')
  void iCanFindPermissionInTheGroupForTheEnvForAppForPortfolio(
      String perm,
      String groupName,
      String envName,
      String appName,
      String portfolioName) async {
    Group group = await _findGroup(portfolioName, groupName, true);
    Application application = await _findApplication(portfolioName, appName);
    Environment environment =
        await common.findExactEnvironment(envName, application.id);
    EnvironmentGroupRole egr =
        group.environmentRoles.firstWhere((environmentRole) {
      return environmentRole.environmentId == environment.id;
    });
    RoleType compareTo = RoleType.READ;
    perm == "EDIT" ? compareTo = RoleType.EDIT : {};
    perm == "LOCK" ? compareTo = RoleType.LOCK : {};
    perm == "UNLOCK" ? compareTo = RoleType.UNLOCK : {};

    assert(
        egr.roles.contains(compareTo), "Permission ${perm} not found in Group");
  }

  @And(
      r'I ensure all permissions added to the group {string} for the env {string} for app {string} for portfolio {string}')
  void iEnsureAllPermissionsAddedToTheGroupForTheEnvForAppForPortfolio(
      String groupName,
      String envName,
      String appName,
      String portfolioName) async {
    Application application = await _findApplication(portfolioName, appName);
    Group group = await _findGroup(portfolioName, groupName, true);
    var environment =
        await common.findExactEnvironment(envName, application.id);
    EnvironmentGroupRole egr = EnvironmentGroupRole();
    egr.groupId = group.id;
    egr.environmentId = environment.id;
    egr.roles.add(RoleType.READ);
    egr.roles.add(RoleType.EDIT);
    egr.roles.add(RoleType.LOCK);
    egr.roles.add(RoleType.UNLOCK);

    group.environmentRoles.add(egr);
    await common.groupService.updateGroup(group.id, group,
        includeGroupRoles: true, updateEnvironmentGroupRoles: true);
    shared.application = application;
  }

  @And(
      r'I ensure that an environment {string} with description {string} exists')
  void iEnsureThatAnEnvironmentWithDescription(
      String envName, String envDesc) async {
    assert(shared.portfolio != null, 'must have portfolio');
    assert(shared.application != null, 'must have application');

    var exists =
        await common.findExactEnvironment(envName, shared.application.id);
    if (exists == null) {
      exists = await common.environmentService.createEnvironment(
          shared.application.id,
          new Environment()
            ..name = envName
            ..description = envDesc);
    }

    assert(exists != null, 'Could not create environment ${envName}');

    shared.environment = exists;
  }

  @And(r'I ensure the permission {string} is added to the group')
  void iEnsureThePermissionIsAddedToTheGroup(String perm) async {
    assert(shared.portfolio != null, 'must have portfolio');
    assert(shared.application != null, 'must have application');
    assert(shared.group != null, 'must have a group');
    assert(shared.environment != null, 'must have an environment');

    final updatedGroup = await common.groupService
        .getGroup(shared.group.id, includeGroupRoles: true);
    final roleType = RoleTypeTypeTransformer.fromJson(perm);
    var eRoles = updatedGroup.environmentRoles.firstWhere(
        (er) => er.environmentId == shared.environment.id,
        orElse: () => null);
    if (eRoles == null) {
      eRoles = EnvironmentGroupRole()
        ..roles = [roleType]
        ..environmentId = shared.environment.id
        ..groupId = shared.group.id;
      updatedGroup.environmentRoles.add(eRoles);
    } else if (!eRoles.roles.contains(roleType)) {
      eRoles.roles.add(roleType);
    }

    await common.groupService.updateGroup(shared.group.id, updatedGroup,
        updateEnvironmentGroupRoles: true);
  }

  @And(r'I ensure that an environments exist:')
  void iEnsureThatAnEnvironmentsExist(GherkinTable table) async {
    assert(shared.portfolio != null, 'Portfolio exists');
    assert(shared.application != null, 'Application exists');

    for (var g in table) {
      final envName = g["name"];
      final envDesc = g["desc"];
      final foundEnv =
          await common.findExactEnvironment(envName, shared.application.id);
      if (foundEnv == null) {
        await common.environmentService.createEnvironment(
            shared.application.id,
            Environment()
              ..name = envName
              ..description = envDesc
              ..applicationId = shared.application.id);
      }
    }
  }

  @And(r'I ensure that environment {string} is before environment {string}')
  void iEnsureThatEnvironmentIsBeforeEnvironment(
      String envFindName, String envUpdatePriorName) async {
    assert(shared.application != null, 'Application exists');
    final appId = shared.application.id;
    Environment envFind = await common.findExactEnvironment(envFindName, appId);
    Environment envUpdatePrior =
        await common.findExactEnvironment(envUpdatePriorName, appId);

    await common.environmentService.updateEnvironment(
        envUpdatePrior.id, envUpdatePrior..priorEnvironmentId = envFind.id);
  }

  @And(r'I check to see that the prior environment for {string} is {string}')
  void iCheckToSeeThatThePriorEnvironmentForIs(
      String envThatHasPriorName, String envThatIsPriorName) async {
    assert(shared.application != null, 'Application exists');
    final appId = shared.application.id;
    Environment envThatHasPrior =
        await common.findExactEnvironment(envThatHasPriorName, appId);
    Environment envThatIsPrior =
        await common.findExactEnvironment(envThatIsPriorName, appId);

    assert(envThatHasPrior.priorEnvironmentId == envThatIsPrior.id,
        'Does not match!');
  }

  @And(r'I check to see that the prior environment for {string} is empty')
  void iCheckToSeeThatThePriorEnvironmentForIsEmpty(String priorEnvName) async {
    assert(shared.application != null, 'Application exists');
    final appId = shared.application.id;
    Environment env = await common.findExactEnvironment(priorEnvName, appId);

    assert(env.priorEnvironmentId == null, 'Prior environment id is empty!');
  }

  @And(r'I delete all existing environments')
  void iDeleteAllExistingEnvironments() async {
    assert(shared.application != null, 'Application does not exist');

    shared.application = await common.applicationService
        .getApplication(shared.application.id, includeEnvironments: true);

    for (var e in shared.application.environments) {
      await common.environmentService.deleteEnvironment(e.id);
    }

    shared.application = await common.applicationService
        .getApplication(shared.application.id, includeEnvironments: true);

    assert(shared.application.environments.length == 0, 'did not delete all environments! ${shared.application.environments}');
  }

  @And(r'I check that environment ordering:')
  void iCheckThatEnvironmentOrdering(GherkinTable table) async {
    assert(shared.application != null, 'Application does not exist');

    shared.application = await common.applicationService
        .getApplication(shared.application.id, includeEnvironments: true);

    for (var e in table) {
      final parent = shared.application.environments
          .firstWhere((en) => en.name == e['parent']);
      final child = e['child'].toString().isEmpty
          ? null
          : shared.application.environments
              .firstWhere((en) => en.name == e['child']);

      if (child == null) {
        assert(
            shared.application.environments.firstWhere(
                    (element) => element.priorEnvironmentId == parent.id,
                    orElse: () => null) ==
                null,
            'Environment ${e["parent"]} is a parent and should not be');
      } else {
        assert(child.priorEnvironmentId == parent.id,
            'Environment child ${child.name} has parent ${shared.application.environments.firstWhere((en) => en.id == child.priorEnvironmentId, orElse: () => null)?.name} which is wrong - should be ${parent.name}');
      }
    }
  }
}
