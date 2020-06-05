import 'package:app_singleapp/shared.dart';
import 'package:app_singleapp/user_common.dart';
import 'package:mrapi/api.dart';
import 'package:ogurets/ogurets.dart';
import 'package:openapi_dart_common/openapi.dart';

class PortfolioStepdefs {
  final Shared shared;
  final UserCommon userCommon;

  PortfolioStepdefs(this.shared, this.userCommon);

  Portfolio found;

  // operates in user space
  @Given(r'I add a group to portfolio {string} with group name {string}')
  void iAddAGroup(String portfolioName, String groupName) async {
    Portfolio p = await userCommon.findExactPortfolio(portfolioName);

    Group group = new Group();
    group.name = groupName;
    Group g = await userCommon.groupService.createGroup(p.id, group);

    assert(g != null);
  }

  // operates in user space
  @And(r'I ensure the {string} user is added to the portfolio admin group for {string}')
  void iAddUserToThePortfolioAdminGroupFor(
      String email, String portfolioName) async {
    Person person = await userCommon.findExactEmail(email);

    assert(person != null, 'Unable to find person');
    assert(shared.portfolioAdminGroup != null,
        'Unable to determine portfolio admin group');

    try {
      await userCommon.groupService
          .addPersonToGroup(shared.portfolioAdminGroup.id, person.id.id);
    } on ApiException catch (e) {
      print("duplcate user, this is ok $e");
    }
  }

  // operates in user space
  @And(r'I confirm that portfolio {string} has a group name {string}')
  void iConfirmThatPortfolioHasAGroupName(
      String portfolioName, String groupName) async {
    Portfolio p = await userCommon.findExactPortfolio(portfolioName);
    assert(p != null, 'Cannot find portfolio $portfolioName');
    List<Group> groups = await userCommon.groupService.findGroups(p.id);
    List<String> groupNames = groups.map((g) => g.name).toList();
    assert(groupNames.contains(groupName),
        'Group name is not contained in ${groupNames}');
  }

  @When(r'I ensure a portfolio {string} has created a group called {string}')
  void iAddAPortfolioHasGroup(String portfolioName, String groupName) async {
    Portfolio p = await userCommon.findExactPortfolio(portfolioName);
    if (!p.groups.map((g) => g.name).toList().contains(groupName)) {
      await userCommon.groupService
          .createGroup(p.id, Group()..name = groupName);
    }
  }

  @Then(r'portfolio {string} has group {string}')
  void portfolioHasGroup(String portfolioName, String groupName) async {
    Portfolio p = await userCommon.findExactPortfolio(portfolioName);
    assert(p.groups.map((g) => g.name).toList().contains(groupName),
        'Group $groupName not in portfolio $portfolioName');
  }

  // access check
  @Then(r'I cannot list any portfolios')
  void iCannotListAnyPortfolios() async {
    var portfolios = null;
    try {
      portfolios = await userCommon.portfolioService.findPortfolios();
    } catch (e) {
      print("failed to get portfolios, this is good.");
    }

    assert(portfolios == null,
        'I was able to get portfolios and should not be allowed to.');
  }

  // this is just an access check
  @And(r'I can list portfolios')
  void iCanListPortfolios() async {
    assert(await userCommon.portfolioService.findPortfolios() != null,
        'I was not able to find any portfolios');
  }

  @When(r'I ensure a portfolio {string} has created a service account called {string}')
  void iEnsureAPortfolioHasCreatedAServiceAccountCalled(
      String portfolioName, String serviceAccountName) async {
    Portfolio p = await userCommon.findExactPortfolio(portfolioName);
    assert(p != null, 'Could not find portfolio group called $portfolioName');
    ServiceAccount sa =
        await userCommon.findExactServiceAccount(serviceAccountName, p.id);
    if (sa == null) {
      ServiceAccount serviceAccount = ServiceAccount();
      serviceAccount.portfolioId = p.id;
      serviceAccount.name = serviceAccountName;
      await userCommon.serviceAccountService
          .createServiceAccountInPortfolio(p.id, serviceAccount);
    }
  }

  @Then(r'portfolio {string} has service account {string}')
  void portfolioHasServiceAccount(
      String portfolioName, String serviceAccountName) async {
    Portfolio p = await userCommon.findExactPortfolio(portfolioName);
    ServiceAccount sa =
        await userCommon.findExactServiceAccount(serviceAccountName, p.id);
    assert(sa != null,
        "I couldn't find the service account " + serviceAccountName);
    assert(sa.apiKey != null, 'API key for Service account $serviceAccountName not set.');
  }

  @When(r'I ensure a portfolio {string} has created application called {string} and environment {string} and service account called {string} and permission type {string}')
  void iEnsureAPortfolioHasCreatedApplicationCalledAndEnvironmentAndServiceAccountCalledAndPermissionType(
      String portfolioName,
      String appName,
      String envName,
      String serviceAccountName,
      String permissionType) async {
    Portfolio p = await userCommon.findExactPortfolio(portfolioName);
    assert(p != null, 'Could not find portfolio group called $portfolioName');
    var app = await userCommon.findExactApplication(appName, p.id);
    var environment;
    if (app == null) {
      app = await userCommon.applicationService.createApplication(p.id,
        Application()
          ..name = appName
          ..description = appName
      );
    }
    assert(app != null, 'Failed to create application');
    environment = await userCommon.findExactEnvironment(envName, app.id);
    if (environment == null) {
      environment = await userCommon.environmentService.createEnvironment(app.id,
        new Environment()
          ..name = envName
          ..description = envName).catchError((e,s){
            print("error creating environment: "+e.toString());
      });
    }
    assert(environment != null, 'Failed to create environment');
    ServiceAccount sa =
        await userCommon.findExactServiceAccount(serviceAccountName, p.id);
    if (sa == null) {
      ServiceAccount serviceAccount = ServiceAccount();
      serviceAccount.portfolioId = p.id;
      serviceAccount.name = serviceAccountName;
      sa = await userCommon.serviceAccountService
          .createServiceAccountInPortfolio(p.id, serviceAccount,includePermissions: true);
    }
    ServiceAccountPermissionType permissionType = ServiceAccountPermissionType.READ;
    if (permissionType == "LOCK") {
      permissionType = ServiceAccountPermissionType.TOGGLE_LOCK;
    } else if (permissionType == "EDIT") {
      permissionType = ServiceAccountPermissionType.TOGGLE_ENABLED;
    }

    ServiceAccountPermission saPermission = sa.permissions.firstWhere((item) =>
    item.environmentId == environment.id, orElse: () =>  null);
    if(saPermission == null){
      saPermission= ServiceAccountPermission();
      List<ServiceAccountPermissionType> permissions = List();
      permissions.add(permissionType);
      saPermission.environmentId= environment.id;
      saPermission.permissions = permissions;
      sa.permissions.add(saPermission);
    } else if (!saPermission.permissions.contains(permissionType)) {
      saPermission.permissions.add(permissionType);
    }
    await userCommon.serviceAccountService.update(sa.id, sa,includePermissions: true);
  }

  @When(r'I ensure a portfolio {string} has created application called {string} and environment {string}')
  void iEnsureAPortfolioHasCreatedApplicationCalledAndEnvironment(String portfolioName,
    String appName, String envName) async {
    Portfolio p = await userCommon.findExactPortfolio(portfolioName);
    assert(p != null, 'Could not find portfolio group called $portfolioName');
    var app = await userCommon.findExactApplication(appName, p.id);
    if (app == null) {
      app = await userCommon.applicationService.createApplication(p.id,
        Application()
          ..name = appName
          ..description = appName
      );
      assert(app != null, 'Failed to create application');
      var exists = await userCommon.findExactEnvironment(envName, app.id);
      if (exists == null) {
        exists = await userCommon.environmentService.createEnvironment(app.id,
          new Environment()
            ..name = envName
            ..description = envName);
      }
    }
  }

  @Then(r'portfolio {string} has service account {string} and the permission {string} for this {string} and {string}')
  void portfolioHasServiceAccountAndThePermissionForThisAnd(String portfolioName,
    String serviceAccountName, String permission, String appName, String envName) async {
    Portfolio p = await userCommon.findExactPortfolio(portfolioName);
    assert(p != null, 'Could not find portfolio group called $portfolioName');
    var app = await userCommon.findExactApplication(appName, p.id);
    assert(app != null, 'Failed to find application');
    var environment = await userCommon.findExactEnvironment(envName, app.id);
    assert(environment != null, 'Failed to find environment');
    ServiceAccount sa =
    await userCommon.findExactServiceAccount(serviceAccountName, p.id);
    assert(sa != null, 'Failed to find service account');
    ServiceAccountPermissionType permissionType = ServiceAccountPermissionType.READ;
    if (permissionType == "LOCK") {
      permissionType = ServiceAccountPermissionType.TOGGLE_LOCK;
    } else if (permissionType == "EDIT") {
      permissionType = ServiceAccountPermissionType.TOGGLE_ENABLED;
    }
    var sap = sa.permissions.firstWhere((item) => item.environmentId == environment.id,
    orElse: () =>  null);
    assert(sap != null, 'Failed to find service account permissions');
    assert(sap.permissions.contains(permissionType));
  }
}
