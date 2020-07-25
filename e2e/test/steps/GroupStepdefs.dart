import 'package:app_singleapp/shared.dart';
import 'package:app_singleapp/user_common.dart';
import 'package:mrapi/api.dart';
import 'package:ogurets/ogurets.dart';
import 'package:openapi_dart_common/openapi.dart';

class GroupStepdefs {
  final UserCommon userCommon;
  final Shared shared;

  GroupStepdefs(this.userCommon, this.shared);

  Future<Group> findCurrentPortfolioGroup() async {
    assert(shared.portfolio != null, 'need to set portfolio');
    assert(shared.person != null, 'need to know person');

    final p = await userCommon.portfolioService
        .getPortfolio(shared.portfolio.id, includeGroups: true);

    final g = p.groups.firstWhere((g) => g.admin);

    final group =
        await userCommon.groupService.getGroup(g.id, includeMembers: true);

    return group;
  }

  @When(r'The portfolio admin group contains the current user')
  void thePortfolioAdminGroupContainsTheCurrentUser() async {
    final group = await findCurrentPortfolioGroup();

    assert(
        group.members.firstWhere((m) => m.id.id == shared.person.id.id,
                orElse: () => null) !=
            null,
        'Group ${group.name} does not contain person ${shared.person.email} ${group.members} vs ${shared.person}');
  }

  @And(r'the shared person is a superuser')
  void theSharedPersonIsASuperuser() async {
    Group superuserGroup = await _findSuperuserGroup();
    assert(
        superuserGroup.members.firstWhere((m) => m.id.id == shared.person.id.id,
                orElse: () => null) !=
            null,
        'Shared person not in superuser group ${shared.person} vs ${superuserGroup}');
  }

  @And(r'I remove the user from the superuser group')
  void iRemoveTheUserFromTheSuperuserGroup() async {
    final sr = await userCommon.setupService.isInstalled();
    final superuserGroup =
        await userCommon.groupService.getSuperuserGroup(sr.organization.id);
    await userCommon.groupService
        .deletePersonFromGroup(superuserGroup.id, shared.person.id.id);
  }

  @And(r'the portfolio admin group does not contain the current user')
  void thePortfolioAdminGroupDoesNotContainTheCurrentUser() async {
    final group = await findCurrentPortfolioGroup();

    assert(
        group.members.firstWhere((m) => m.id.id == shared.person.id.id,
                orElse: () => null) ==
            null,
        'Group ${group.name} does contain person ${shared.person.email} (and should not) ${group.members} vs ${shared.person}');
  }

  @And(r'I fail to remove the shared user from the portfolio group')
  void iRemoveTheUserFromThePortfolioGroup() async {
    final group = await findCurrentPortfolioGroup();

    try {
      await userCommon.groupService
          .deletePersonFromGroup(group.id, shared.person.id.id);
      assert(true == false, 'Should not have succeeded');
    } catch (e) {
      assert(e is ApiException && e.code == 404,
          'failed for some other reason $e');
    }
  }

  Future<Group> _findSuperuserGroup() async {
    final sr = await userCommon.setupService.isInstalled();
    return await userCommon.groupService.getSuperuserGroup(sr.organization.id);
  }

  @When(
      r'I add the shared person to the superuser group via the group membership')
  void iAddTheSharedPersonToTheSuperuserGroup() async {
    Group superuserGroup = await _findSuperuserGroup();
    superuserGroup.members.add(shared.person);
    await userCommon.groupService
        .updateGroup(superuserGroup.id, superuserGroup, updateMembers: true);
  }

  @When(
      r'I remove the shared person to the superuser group via the group membership')
  void iRemoveTheSharedPersonToTheSuperuserGroup() async {
    Group superuserGroup = await _findSuperuserGroup();
    // remove this specific user
    superuserGroup.members.retainWhere((m) => m.id.id != shared.person.id.id);
    await userCommon.groupService
        .updateGroup(superuserGroup.id, superuserGroup, updateMembers: true);
  }

  @And(
      r'I attempt to remove the superuser from the shared portfolio group via the group membership')
  void
      iAttemptToRemoveTheSuperuserFromTheSharedPortfolioGroupViaTheGroupMembership() async {
    final group = await findCurrentPortfolioGroup();
    group.members.retainWhere((m) => m.id.id != shared.person.id.id);
    await userCommon.groupService
        .updateGroup(group.id, group, updateMembers: true);
  }

  @And(r'I add the shared user to the current portfolio admin group')
  void iAddTheSharedUserToTheCurrentPortfolioAdminGroup() async {
    final group = await findCurrentPortfolioGroup();
    await userCommon.groupService
        .addPersonToGroup(group.id, shared.person.id.id);
  }
}
