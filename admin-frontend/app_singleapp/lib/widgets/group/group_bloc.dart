import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

class GroupBloc implements Bloc {
  String groupId;
  Group group;
  String search;
  final ManagementRepositoryClientBloc mrClient;
  GroupServiceApi _groupServiceApi;

  Stream<Group> get groupLoaded => _groupSource.stream;
  final _groupSource = BehaviorSubject<Group>();

  GroupBloc(this.groupId, this.mrClient) : assert(mrClient != null) {
    _groupServiceApi = GroupServiceApi(mrClient.apiClient);
    mrClient.streamValley.currentPortfolioIdStream.listen((event) {
      if (!_groupSource.isClosed) {
        _groupSource.add(null);
      }
      groupId = null;
      group = null;
    });
  }

  Future<void> getGroups({Group focusGroup}) async {
    await mrClient.streamValley.getCurrentPortfolioGroups();
    if (!_groupSource.isClosed) {
      _groupSource.add(focusGroup);
    }
  }

  void getGroup(String groupId) async {
    if (groupId != null && groupId.length > 1) {
      Group fetchedGroup = await _groupServiceApi
          .getGroup(groupId, includeMembers: true)
          .catchError(mrClient.dialogError);
      // publish it out...
      if (fetchedGroup != null) {
        this.group = fetchedGroup;
        _groupSource.add(fetchedGroup);
      }
    }
  }

  Future<void> deleteGroup(String groupId, bool includeMembers) async {
    await _groupServiceApi
        .deleteGroup(groupId, includeMembers: includeMembers)
        .catchError(mrClient.dialogError);
    this.group = null;
    this.groupId = null;
    _groupSource.add(null);
    await mrClient.streamValley.getCurrentPortfolioGroups();
  }

  void removeFromGroup(Group group, Person person) async {
    var data = await _groupServiceApi
        .deletePersonFromGroup(group.id, person.id.id, includeMembers: true);
    if (!_groupSource.isClosed) {
      _groupSource.add(data);
    }
  }

  Future<void> updateGroup(Group groupToUpdate) async {
    await _groupServiceApi
        .updateGroup(groupToUpdate.id, groupToUpdate,
            includeMembers: true, updateMembers: true)
        .catchError(mrClient.dialogError);
    await getGroups(focusGroup: groupToUpdate);
    this.group = groupToUpdate;
    this.groupId = groupToUpdate.id;
  }

  Future<void> createGroup(Group newGroup) async {
    Group createdGroup = await _groupServiceApi
        .createGroup(mrClient.currentPid, newGroup)
        .catchError(mrClient.dialogError);
    await getGroups(focusGroup: createdGroup);
    this.groupId = createdGroup.id;
    this.group = createdGroup;
    return newGroup;
  }

  @override
  void dispose() {
    _groupSource.close();
  }
}
