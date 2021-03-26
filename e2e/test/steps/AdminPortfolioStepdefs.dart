import 'package:collection/collection.dart' show IterableExtension;
import 'package:e2e_tests/shared.dart';
import 'package:e2e_tests/superuser_common.dart';
import 'package:e2e_tests/user_common.dart';
import 'package:mrapi/api.dart';
import 'package:ogurets/ogurets.dart';
import 'package:openapi_dart_common/openapi.dart';

class AdminPortfolioStepdefs {
  final SuperuserCommon common;
  final Shared shared;
  final UserCommon userCommon;

  AdminPortfolioStepdefs(this.common, this.shared, this.userCommon);

  // only superusers can do this, but it can run in user space if the user is a superuser
  @Given(
      r'I ensure a portfolio named {string} with description {string} exists')
  void iCreateANewPortfolioCalledWithGroup(
      String portfolioName, String desc) async {
    await common.initialize();

    var portfolio = await userCommon.findExactPortfolio(portfolioName,
        portfolioServiceApi: common.portfolioService);

    if (portfolio == null) {
      Portfolio p = await common.portfolioService.createPortfolio(
          Portfolio(description: desc, name: portfolioName),
          includeGroups: true);

      assert(p.groups!.length == 1);

      shared.portfolio = p;
      shared.portfolioAdminGroup = p.groups![0];
    } else {
      shared.portfolioAdminGroup =
          portfolio.groups!.firstWhere((g) => g.admin!);
      shared.portfolio = portfolio;
    }
  }

  @Given(r'I ensure that a portfolio {string} has been deleted')
  void iEnsureThatAPortfolioHasBeenDeleted(String portfolioName) async {
    await common.initialize();

    var portfolio = await userCommon.findExactPortfolio(portfolioName,
        portfolioServiceApi: common.portfolioService);
    if (portfolio != null) {
      await common.portfolioService.deletePortfolio(portfolio.id!);
      portfolio = await userCommon.findExactPortfolio(portfolioName,
          portfolioServiceApi: common.portfolioService);
      assert(portfolio == null, 'failed to delete portfolio');
    }
  }

  @And(
      r'I update the portfolio group {string} to the name {string} with the description {string}')
  void iUpdateThePortfolioGroupToTheNameWithTheDescription(
      String portfolioName, String newName, String newDesc) async {
    await common.initialize();

    var portfolio = await userCommon.findExactPortfolio(portfolioName,
        portfolioServiceApi: common.portfolioService);
    assert(portfolio != null, 'Cannot find portfolio to update');
    if (portfolio != null) {
      await common.portfolioService.updatePortfolio(
          portfolio.id!,
          Portfolio(name: newName, description: newDesc)
            ..version = portfolio.version,
          includeGroups: true);
    }
  }

  @Then(r'I cannot create a portfolio named {string} with description {string}')
  void iCannotCreateAPortfolioNamedWithDescription(
      String portfolioName, String description) async {
    await common.initialize();

    var p = null;
    try {
      p = await common.portfolioService.createPortfolio(
          new Portfolio(name: portfolioName, description: description));
    } catch (e) {
      assert(e is ApiException && e.code == 409,
          'Expecting a conflict but did not receive one.');
    }

    assert(p == null, 'Should not have been able to create portfolio');
  }

  @Then(r'I cannot rename portfolio {string} to {string}')
  void iCannotRenamePortfolioTo(String portfolioName, String newName) async {
    await common.initialize();

    var existing = await userCommon.findExactPortfolio(portfolioName,
        portfolioServiceApi: common.portfolioService);

    assert(existing != null, 'Cannot find portfolio to rename');

    var p = null;
    try {
      p = await common.portfolioService.updatePortfolio(
          existing!.id!,
          new Portfolio(
              name: newName,
              description: 'not important',
              version: existing.version));
    } catch (e) {
      assert(e is ApiException && e.code == 409,
          'Expecting a conflict but did not receive one.');
    }

    assert(p == null, 'Should not have been able to update portfolio');
  }

  @When(
      r'I add the user {string} to the group {string} in the portfolio {string}')
  void iAddTheUserToTheGroupInThePortfolio(
      String email, String groupName, String portfolioName) async {
    await common.initialize();

    var portfolio = await userCommon.findExactPortfolio(portfolioName,
        portfolioServiceApi: common.portfolioService);
    assert(portfolio != null, 'No such portfolio $portfolioName');

    var matchedGroup = await userCommon.findExactGroup(groupName, portfolio!.id,
        groupServiceApi: common.groupService);

    assert(matchedGroup != null, 'Cannot find group $groupName');

    var person = await userCommon.findExactEmail(email,
        personServiceApi: common.personService);

    assert(person != null, 'could not find email/person $email');

    await common.groupService.addPersonToGroup(
        matchedGroup!.id!, person!.id!.id,
        includeMembers: true);

    shared.portfolio = portfolio;
    shared.group = matchedGroup;
    shared.person = person;
  }

  @Then(r'Searching for user should include the group')
  void searchingForUserShouldIncludeTheGroupInThePortfolio() async {
    await common.initialize();
    var person = await common.personService
        .getPerson(shared.person.id!.id, includeGroups: true);
    assert(person.groups!.isNotEmpty, 'Person has no groups');
    assert(
        person.groups!.firstWhereOrNull((g) => g.id == shared.group.id) != null,
        'Could not find group ${shared.group} in person ${person}');
  }

  @And(r'Searching for the group should include the user')
  void searchingForTheGroupShouldIncludeTheUser() async {
    await common.initialize();
    var group = await common.groupService
        .getGroup(shared.group.id!, includeMembers: true);
    assert(group.members!.isNotEmpty, 'Group has no members');
    assert(
        group.members!
                .firstWhereOrNull((p) => p.id!.id == shared.person.id!.id) !=
            null,
        'Could not find person ${shared.person} in group ${group}');
  }

  @Given(r'I have a randomly named portfolio with the prefix {string}')
  void iHaveARandomlyNamedPortfolioWithThePrefix(String portfolioPrefix) async {
    String portfolioName = portfolioPrefix +
        '_' +
        DateTime.now().millisecondsSinceEpoch.toString();

    shared.portfolio = await common.portfolioService.createPortfolio(
        new Portfolio(name: portfolioName, description: 'A random portfolio'));
  }
}
