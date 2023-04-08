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
        final fetchedGroup = await _groupServiceApi
            .getGroup(groupId, includeMembers: true);
        // publish it out...
        group = fetchedGroup;
        _groupSource.add(fetchedGroup);
      } catch (e,s) {
        mrClient.dialogError(e, s);
      }
    }
  }

  Future<void> deleteGroup(String groupId, bool includeMembers) async {
    try {
      await _groupServiceApi
          .deleteGroup(groupId, includeMembers: includeMembers);
      group = null;
      this.groupId = null;
      _groupSource.add(null);
      await mrClient.streamValley.getCurrentPortfolioGroups(force: true);
    } catch (e, s) {
      mrClient.dialogError(e, s);
    }
  }

  void removeFromGroup(Group group, Person person) async {
    var data = await _groupServiceApi
        .deletePersonFromGroup(group.id!, person.id!.id, includeMembers: true);
    if (!_groupSource.isClosed) {
      _groupSource.add(data);
    }
  }

  Future<bool> updateGroup(Group groupToUpdate) async {
    try {
      await _groupServiceApi.updateGroup(groupToUpdate.id!, groupToUpdate,
          includeMembers: true, updateMembers: true);
      await getGroups(focusGroup: groupToUpdate);
      group = groupToUpdate;
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

  Future<void> createGroup(Group newGroup) async {
    final createdGroup = await _groupServiceApi
        .createGroup(mrClient.currentPid!, newGroup);
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
