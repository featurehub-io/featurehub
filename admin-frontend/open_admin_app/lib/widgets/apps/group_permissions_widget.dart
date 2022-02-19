import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_footer_button_bar.dart';
import 'package:open_admin_app/widgets/common/fh_underline_button.dart';

import 'manage_app_bloc.dart';

class GroupPermissionsWidget extends StatelessWidget {
  const GroupPermissionsWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ManageAppBloc>(context);
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    return StreamBuilder<List<Group>>(
        stream: bloc.groupsStream,
        builder: (context, snapshot) {
          if (!snapshot.hasData || snapshot.hasError) {
            return Container(
              padding: const EdgeInsets.all(30),
              child: const Text('Loading...'),
            );
          }

          return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.end,
              children: <Widget>[
                const SizedBox(
                  height: 16.0,
                ),
                Row(
                  children: [
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Group',
                          style: Theme.of(context).textTheme.caption,
                        ),
                        _GroupsDropdown(groups: snapshot.data!, bloc: bloc),
                      ],
                    ),
                    const SizedBox(width: 16.0),
                    FHUnderlineButton(
                      title: 'Go to groups settings',
                      keepCase: true,
                      onPressed: () {
                        ManagementRepositoryClientBloc.router
                            .navigateTo(context, '/groups', params: {
                          'id': [bloc.selectedGroup!]
                        });
                      },
                    ),
                  ],
                ),
                _GroupPermissionDetailWidget(bloc: bloc, mr: mrBloc)
              ]);
        });
  }
}

class _GroupsDropdown extends StatefulWidget {
  final List<Group> groups;
  final ManageAppBloc bloc;

  const _GroupsDropdown({Key? key, required this.groups, required this.bloc})
      : super(key: key);

  @override
  __GroupsDropdownState createState() => __GroupsDropdownState();
}

class __GroupsDropdownState extends State<_GroupsDropdown> {
  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(maxWidth: 250),
      child: InkWell(
        mouseCursor: SystemMouseCursors.click,
        child: DropdownButton(
          icon: const Padding(
            padding: EdgeInsets.only(left: 8.0),
            child: Icon(
              Icons.keyboard_arrow_down,
              size: 18,
            ),
          ),
          isDense: true,
          isExpanded: true,
          items: widget.groups.map((Group group) {
            return DropdownMenuItem<String>(
                value: group.id,
                child: Text(
                  group.name,
                  style: Theme.of(context).textTheme.bodyText2,
                  overflow: TextOverflow.ellipsis,
                ));
          }).toList(),
          hint: const Text('Select group'),
          onChanged: (String? value) {
            if (value != null) {
              setState(() {
                widget.bloc.selectedGroup = value;
              });
            }
          },
          value: widget.bloc.selectedGroup,
        ),
      ),
    );
  }
}

class _GroupPermissionDetailWidget extends StatefulWidget {
  final ManagementRepositoryClientBloc mr;
  final ManageAppBloc bloc;

  const _GroupPermissionDetailWidget({
    Key? key,
    required this.mr,
    required this.bloc,
  }) : super(key: key);

  @override
  _GroupPermissionDetailState createState() => _GroupPermissionDetailState();
}

class _GroupPermissionDetailState extends State<_GroupPermissionDetailWidget> {
  Map<String, EnvironmentGroupRole> newEnvironmentRoles = {};
  Group? currentGroup;
  String? applicationId;
  bool editAccess = false;

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<ApplicationGroupRoles?>(
        stream: widget.bloc.groupRoleStream,
        builder: (context, groupSnapshot) {
          if (!groupSnapshot.hasData) {
            return Container(
                padding: const EdgeInsets.all(20),
                child: const Text(
                    'You need to select a group to edit the permissions for.'));
          }

          return StreamBuilder<List<Environment>>(
              stream: widget.bloc.environmentsStream,
              builder: (context, envSnapshot) {
                if (!envSnapshot.hasData || envSnapshot.data!.isEmpty) {
                  return Row(
                    mainAxisAlignment: MainAxisAlignment.start,
                    children: <Widget>[
                      Container(
                          padding: const EdgeInsets.all(20),
                          child: const Text(
                              "You need to first create some 'Environments' for this application.")),
                    ],
                  );
                }

                if (currentGroup == null ||
                    (currentGroup!.id != groupSnapshot.data!.group.id) ||
                    (applicationId != groupSnapshot.data!.applicationId)) {
                  newEnvironmentRoles =
                      createMap(envSnapshot.data!, groupSnapshot.data!.group);
                  currentGroup = groupSnapshot.data?.group;
                  applicationId = groupSnapshot.data!.applicationId;
                  editAccess = hasEditPermission(
                      currentGroup!, widget.bloc.applicationId!);
                }

                final rows = <TableRow>[];
                rows.add(getHeader());
                for (var env in envSnapshot.data!) {
                  rows.add(TableRow(
                      decoration: BoxDecoration(
                          border: Border(
                              bottom: BorderSide(
                                  color: Theme.of(context).dividerColor))),
                      children: [
                        Container(
                            padding: const EdgeInsets.fromLTRB(5, 15, 0, 0),
                            child: Text(env.name)),
                        getPermissionCheckbox(env.id!, RoleType.READ),
                        getPermissionCheckbox(env.id!, RoleType.LOCK),
                        getPermissionCheckbox(env.id!, RoleType.UNLOCK),
                        getPermissionCheckbox(env.id!, RoleType.CHANGE_VALUE),
                      ]));
                }

                return Column(
                  children: <Widget>[
                    Row(
                      children: <Widget>[
                        Checkbox(
                            value: editAccess,
                            onChanged: (value) {
                              if (value != null) {
                                setState(() {
                                  editAccess = value;
                                });
                              }
                            }),
                        Text(
                            'This group can create, edit and delete features for this application',
                            style: Theme.of(context).textTheme.caption),
                      ],
                    ),
                    Container(
                        padding: const EdgeInsets.fromLTRB(0, 16, 0, 16),
                        child: Center(
                          child: Text(
                              'Set the group access to features for each environment',
                              style: Theme.of(context).textTheme.caption),
                        )),
                    Table(children: rows),
                    FHButtonBar(children: [
                      FHFlatButtonTransparent(
                        onPressed: () {
                          currentGroup = null;
                          widget.bloc.resetGroup(groupSnapshot.data!.group);
                        },
                        title: 'Cancel',
                        keepCase: true,
                      ),
                      FHFlatButton(
                          onPressed: () {
                            final newList = <EnvironmentGroupRole>[];
                            newEnvironmentRoles.forEach((key, value) {
                              newList.add(value);
                            });
                            var newGroup = groupSnapshot.data!.group;
                            newGroup.environmentRoles = newList;
                            newGroup = editAccess
                                ? addEditPermission(newGroup, applicationId!)
                                : removeEditPermission(
                                    newGroup, applicationId!);
                            widget.bloc
                                .updateGroupWithEnvironmentRoles(
                                    newGroup.id, newGroup)
                                .then((group) => widget.bloc.mrClient
                                    .addSnackbar(
                                        Text("Group '${group.name}' updated!")))
                                .catchError((e, s) {
                              widget.bloc.mrClient.dialogError(e, s);
                            });
                          },
                          title: 'Update'),
                    ])
                  ],
                );
              });
        });
  }

  TableRow getHeader() {
    return TableRow(
        decoration: BoxDecoration(
            border: Border(
                bottom: BorderSide(color: Theme.of(context).dividerColor))),
        children: [
          Container(
            padding: const EdgeInsets.fromLTRB(5, 0, 0, 15),
            child: const Text(
              '',
            ),
          ),
          Center(
              child: Text(
            'Read',
            style: Theme.of(context).textTheme.subtitle2,
          )),
          Center(
              child: Text(
            'Lock',
            style: Theme.of(context).textTheme.subtitle2,
          )),
          Center(
              child: Text(
            'Unlock',
            style: Theme.of(context).textTheme.subtitle2,
          )),
          Center(
              child: Text(
            'Change value / Retire',
            style: Theme.of(context).textTheme.subtitle2,
          )),
        ]);
  }

  Checkbox getPermissionCheckbox(String envId, RoleType roleType) {
    return Checkbox(
      value: newEnvironmentRoles.containsKey(envId) &&
          newEnvironmentRoles[envId]!.roles.contains(roleType),
      onChanged: (value) {
        setState(() {
          if (value == true) {
            newEnvironmentRoles[envId]!.roles.add(roleType);
          } else {
            newEnvironmentRoles[envId]!.roles.remove(roleType);
          }
        });
      },
    );
  }

  bool hasEditPermission(Group group, String aid) {
    final agr = group.applicationRoles.firstWhereOrNull(
        (item) => item.applicationId == aid && item.groupId == group.id);

    if (agr == null || !agr.roles.contains(ApplicationRoleType.FEATURE_EDIT)) {
      return false;
    }
    return true;
  }

  Group addEditPermission(Group group, String aid) {
    if (!hasEditPermission(group, aid)) {
      final agr = ApplicationGroupRole(
          applicationId: aid,
          groupId: group.id!,
          roles: [ApplicationRoleType.FEATURE_EDIT]);
      group.applicationRoles.add(agr);
    }
    return group;
  }

  Group removeEditPermission(Group group, String aid) {
    group.applicationRoles.removeWhere(
        (item) => item.applicationId == aid && item.groupId == group.id);
    return group;
  }

  Map<String, EnvironmentGroupRole> createMap(
      List<Environment> environments, Group group) {
    final retMap = <String, EnvironmentGroupRole>{};
    for (var environment in environments) {
      var egr = group.environmentRoles.firstWhereOrNull(
          (environmentRole) => environmentRole.environmentId == environment.id);

      if (egr == null) {
        egr = EnvironmentGroupRole(
            environmentId: environment.id!, groupId: group.id!, roles: []);
      } else {
        egr.environmentId = environment.id!;
        egr.groupId = group.id!;
      }

      retMap[environment.id!] = egr;
    }
    return retMap;
  }
}
