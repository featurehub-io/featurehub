import 'package:collection/collection.dart' show IterableExtension;
import 'package:e2e_tests/shared.dart';
import 'package:e2e_tests/superuser_common.dart';
import 'package:e2e_tests/user_common.dart';
import 'package:mrapi/api.dart';
import 'package:ogurets/ogurets.dart';
import 'package:openapi_dart_common/openapi.dart';

class GroupStepdefs {
  final SuperuserCommon common;
  final UserCommon userCommon;
  final Shared shared;

  GroupStepdefs(this.userCommon, this.shared, this.common);

  Future<Group> findCurrentPortfolioGroup() async {
    final p = await userCommon.portfolioService
        .getPortfolio(shared.portfolio.id, includeGroups: true);

    final g = p.groups.firstWhere((g) => g.admin!);

    final group =
        await userCommon.groupService.getGroup(g.id, includeMembers: true);

    return group;
  }

  @When(r'The portfolio admin group contains the current user')
  void thePortfolioAdminGroupContainsTheCurrentUser() async {
    final group = await findCurrentPortfolioGroup();

    assert(
        group.members
                .firstWhereOrNull((m) => m.id!.id == shared.person.id!.id) !=
            null,
        'Group ${group.name} does not contain person ${shared.person.email} ${group.members} vs ${shared.person}');
  }

  @And(r'the shared person is a superuser')
  void theSharedPersonIsASuperuser() async {
    final currentPerson = shared.person.id!.id;
    Group? superuserGroup = await _findSuperuserGroup(currentPerson);
    assert(superuserGroup != null,
        'Shared person not in superuser group ${shared.person} vs ${superuserGroup}');
  }

  Future<Group?> _findSuperuserGroup(String currentPerson) async {
    final personsGroups = await userCommon.personService.getPerson(currentPerson, includeGroups: true);

    return personsGroups.groups.firstWhereOrNull((g) => g.portfolioId == null && g.admin == true);
  }

  @And(r'I remove the user from the superuser group')
  void iRemoveTheUserFromTheSuperuserGroup() async {
    final sr = await userCommon.setupService.isInstalled();
    final personToDelete = shared.person.id!.id;

    final suGroup = await _findSuperuserGroup(personToDelete);

    if (suGroup != null) {
      await userCommon.groupService
          .deletePersonFromGroup(suGroup.id, personToDelete);
    }
  }

  @And(r'the portfolio admin group does not contain the current user')
  void thePortfolioAdminGroupDoesNotContainTheCurrentUser() async {
    final group = await findCurrentPortfolioGroup();

    assert(
        group.members
                .firstWhereOrNull((m) => m.id!.id == shared.person.id!.id) ==
            null,
        'Group ${group.name} does contain person ${shared.person.email} (and should not) ${group.members} vs ${shared.person}');
  }

  @And(r'I fail to remove the shared user from the portfolio group')
  void iRemoveTheUserFromThePortfolioGroup() async {
    final group = await findCurrentPortfolioGroup();

    try {
      await userCommon.groupService
          .deletePersonFromGroup(group.id, shared.person.id!.id);
      assert(true == false, 'Should not have succeeded');
    } catch (e) {
      assert(e is ApiException && e.code == 404,
          'failed for some other reason $e');
    }
  }

  @When(
      r'I add the shared person to the superuser group via the group membership')
  void iAddTheSharedPersonToTheSuperuserGroup() async {
    Group? superuserGroup = await _findSuperuserGroup(common.superuserId);
    assert(superuserGroup != null, 'Superuser cannot find their own group!');
    superuserGroup!.members.add(shared.person);
    await userCommon.groupService.addPersonToGroup(superuserGroup.id, shared.person.id!.id);
  }

  @When(
      r'I remove the shared person to the superuser group via the group membership')
  void iRemoveTheSharedPersonToTheSuperuserGroup() async {
    Group? superuserGroup = await _findSuperuserGroup(common.superuserId);
    assert(superuserGroup != null, 'Superuser cannot find their own group!');
    // remove this specific user
    superuserGroup!.members.retainWhere((m) => m.id!.id != shared.person.id!.id);
    await userCommon.groupService.deletePersonFromGroup(superuserGroup.id, shared.person.id!.id);
  }

  @And(
      r'I attempt to remove the superuser from the shared portfolio group via the group membership')
  void
      iAttemptToRemoveTheSuperuserFromTheSharedPortfolioGroupViaTheGroupMembership() async {
    final group = await findCurrentPortfolioGroup();
    group.members.retainWhere((m) => m.id!.id != shared.person.id!.id);
    await userCommon.groupService.updateGroupOnPortfolio(group.portfolioId!, group, updateMembers: true);
  }

  @And(r'I add the shared user to the current portfolio admin group')
  void iAddTheSharedUserToTheCurrentPortfolioAdminGroup() async {
    final group = await findCurrentPortfolioGroup();
    await userCommon.groupService
        .addPersonToGroup(group.id, shared.person.id!.id);
  }
}
