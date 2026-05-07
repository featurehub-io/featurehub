import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:rxdart/rxdart.dart';

class GroupBloc implements Bloc {
  String? groupId;
  Group? group;
  String? search;
  final ManagementRepositoryClientBloc mrClient;
  StreamSubscription<List<Group>>? _groupListener;

  Stream<Group?> get groupLoaded => _groupSource.stream;
  final _groupSource = BehaviorSubject<Group?>();

  GroupServiceApi get _groupServiceApi => mrClient.groupServiceApi;

  GroupBloc(this.groupId, this.mrClient) {
    _groupListener =
        mrClient.streamValley.currentPortfolioGroupsStream.listen((groups) {
      final ourGroup = groups.firstWhereOrNull((g) => g.id == groupId);
      if (ourGroup == null) {
        if (groups.isNotEmpty) {
          group = groups.first;
          groupId = group!.id;
          _groupSource.add(group);
          getGroup(groupId);
        } else {
          groupId = null;
          group = null;
          _groupSource.add(null);
        }
      } else {
        // in case something changed
        group = ourGroup;
        _groupSource.add(group);
        getGroup(groupId);
      }
    });
  }

  Future<void> getGroups({required Group focusGroup}) async {
    // refresh the groups
    final refreshedGroups =
        await mrClient.streamValley.getCurrentPortfolioGroups(force: true);
    if (!_groupSource.isClosed) {
      _groupSource
          .add(refreshedGroups.firstWhereOrNull((g) => g.id == focusGroup.id));
    }
  }

  Future<void> getGroup(String? groupId) async {
    if (groupId != null && groupId.length > 1) {
      try {
        final fetchedGroup =
            await _groupServiceApi.getGroup(groupId, includeMembersV2: true);
        // publish it out...
        group = fetchedGroup;
        _groupSource.add(fetchedGroup);
      } catch (e, s) {
        mrClient.dialogError(e, s);
      }
    }
  }

  Future<void> deleteGroup(String groupId, bool includeMembers) async {
    try {
      await _groupServiceApi.deleteGroup(groupId,
          includeMembersV2: includeMembers);
      group = null;
      this.groupId = null;
      _groupSource.add(null);
      await mrClient.streamValley.getCurrentPortfolioGroups(force: true);
    } catch (e, s) {
      mrClient.dialogError(e, s);
    }
  }

  Future<void> removeFromGroup(Group group, String personId) async {
    var data = await _groupServiceApi
        .deletePersonFromGroup(group.id, personId, includeMembersV2: true);
    if (!_groupSource.isClosed) {
      _groupSource.add(data);
    }
  }

  Future<bool> addUsersToGroup(Group group, List<Person> persons) async {
    try {
      final fetchedGroup = await _groupServiceApi.addPersonsToGroup(group.id, personId: persons.map((p) => p.id!.id).toList(), includeMembersV2: true);
      group = fetchedGroup;
      _groupSource.add(fetchedGroup);
      return true;
    } catch (e, s) {
      await mrClient.dialogError(e, s,
          messageTitle: 'Failed to add users to group',
          messageBody:
          'Failed to update group because of a duplicate or other conflict.');
      return false;
    }
  }

  Future<bool> updateGroupName(Group groupToUpdate, String name) async {
    try {
      groupToUpdate.name = name;

      final newGroup = await _groupServiceApi.updateGroupOnPortfolio(
          mrClient.currentPortfolio!.id, groupToUpdate,
          includeMembersV2: true, updateMembers: false);

      // tell the portfolio groups to update as the name has changed.
      await getGroups(focusGroup: groupToUpdate);

      group = newGroup;
      _groupSource.add(newGroup);
      groupId = groupToUpdate.id;
      return true;
    } catch (e, s) {
      await mrClient.dialogError(e, s,
          messageTitle: 'Failed to update group',
          messageBody:
              'Failed to update group because of a duplicate or other conflict.');
      return false;
    }
  }

  Future<void> createGroup(String name) async {
    final createdGroup = await _groupServiceApi.createGroup(
        mrClient.currentPid!, CreateGroup(name: name));
    await getGroups(focusGroup: createdGroup);
    groupId = createdGroup.id;
    group = createdGroup;
  }

  @override
  void dispose() {
    _groupListener?.cancel();
    _groupSource.close();
  }
}
