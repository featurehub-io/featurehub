import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/common/fh_footer_button_bar.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';

import 'manage_app_bloc.dart';

class GroupPermissionsWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ManageAppBloc>(context);
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    return StreamBuilder<List<Group>>(
        stream: bloc.groupsStream,
        builder: (context, snapshot) {
          if (!snapshot.hasData || snapshot.hasError) {
            return Container(
              padding: EdgeInsets.all(30),
              child: Text('Loading...'),
            );
          }

          return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.end,
              children: <Widget>[
                SizedBox(
                  height: 16.0,
                ),
                Row(
                  children: [
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Container(
                            child: Text(
                          'Group',
                          style: Theme.of(context).textTheme.caption,
                        )),
                        Container(
                            child: _GroupsDropdown(
                                groups: snapshot.data, bloc: bloc)),
                      ],
                    ),
                    SizedBox(width: 16.0),
                    FHFlatButtonTransparent(
                      title: 'Manage group members',
                      keepCase: true,
                      onPressed: () {
                        ManagementRepositoryClientBloc.router.navigateTo(
                            context, '/manage-group',
                            transition: TransitionType.material,
                            params: {
                              'id': [bloc.selectedGroup]
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

  const _GroupsDropdown({Key? key, this.groups, this.bloc}) : super(key: key);

  @override
  __GroupsDropdownState createState() => __GroupsDropdownState();
}

class __GroupsDropdownState extends State<_GroupsDropdown> {
  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: BoxConstraints(maxWidth: 250),
      child: InkWell(
        mouseCursor: SystemMouseCursors.click,
        child: DropdownButton(
          icon: Padding(
            padding: EdgeInsets.only(left: 8.0),
            child: Icon(
              Icons.keyboard_arrow_down,
              size: 24,
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
          hint: Text('Select group'),
          onChanged: (value) {
            setState(() {
              widget.bloc.selectedGroup = value;
            });
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
  })   : assert(mr != null),
        assert(bloc != null),
        super(key: key);

  @override
  _GroupPermissionDetailState createState() => _GroupPermissionDetailState();
}

class _GroupPermissionDetailState extends State<_GroupPermissionDetailWidget> {
  Map<String, EnvironmentGroupRole> newEnvironmentRoles = {};
  Group currentGroup;
  bool editAccess;

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<Group>(
        stream: widget.bloc.groupRoleStream,
        builder: (context, groupSnapshot) {
          if (!groupSnapshot.hasData) {
            return Container(
                padding: EdgeInsets.all(20),
                child: Text(
                    'You need to select a group to edit the permissions for.'));
          }

          return StreamBuilder<List<Environment>>(
              stream: widget.bloc.environmentsStream,
              builder: (context, envSnapshot) {
                if (!envSnapshot.hasData || envSnapshot.data.isEmpty) {
                  return Row(
                    mainAxisAlignment: MainAxisAlignment.start,
                    children: <Widget>[
                      Container(
                          padding: EdgeInsets.all(20),
                          child: Text(
                              "You need to first create some 'Environments' for this application.")),
                    ],
                  );
                }

                if (currentGroup == null ||
                    currentGroup.id != groupSnapshot.data.id) {
                  newEnvironmentRoles =
                      createMap(envSnapshot.data, groupSnapshot.data);
                  currentGroup = groupSnapshot.data;
                  editAccess = hasEditPermission(
                      currentGroup, widget.bloc.application.id);
                }

                final rows = <TableRow>[];
                rows.add(getHeader());
                for (var env in envSnapshot.data) {
                  rows.add(TableRow(
                      decoration: BoxDecoration(
                          border: Border(
                              bottom: BorderSide(
                                  color: Theme.of(context).dividerColor))),
                      children: [
                        Container(
                            padding: EdgeInsets.fromLTRB(5, 15, 0, 0),
                            child: Text(env.name)),
                        getPermissionCheckbox(env.id, RoleType.READ),
                        getPermissionCheckbox(env.id, RoleType.LOCK),
                        getPermissionCheckbox(env.id, RoleType.UNLOCK),
                        getPermissionCheckbox(env.id, RoleType.CHANGE_VALUE),
                      ]));
                }

                return Column(
                  children: <Widget>[
                    Row(
                      children: <Widget>[
                        Checkbox(
                            value: editAccess,
                            onChanged: (value) {
                              setState(() {
                                editAccess = value;
                              });
                            }),
                        Text(
                            'This group can create, edit and delete features for this application',
                            style: Theme.of(context).textTheme.caption),
                      ],
                    ),
                    Container(
                        padding: EdgeInsets.fromLTRB(0, 16, 0, 16),
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
                          widget.bloc.resetGroup(groupSnapshot.data);
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
                            var newGroup = groupSnapshot.data;
                            newGroup.environmentRoles = newList;
                            newGroup = editAccess
                                ? addEditPermission(
                                    newGroup, widget.bloc.application.id)
                                : removeEditPermission(
                                    newGroup, widget.bloc.application.id);
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
            padding: EdgeInsets.fromLTRB(5, 0, 0, 15),
            child: Text(
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
            'Change value',
            style: Theme.of(context).textTheme.subtitle2,
          )),
        ]);
  }

  Checkbox getPermissionCheckbox(String envId, RoleType roleType) {
    return Checkbox(
      value: newEnvironmentRoles[envId].roles.contains(roleType),
      onChanged: (value) {
        setState(() {
          if (value) {
            newEnvironmentRoles[envId].roles.add(roleType);
          } else {
            newEnvironmentRoles[envId].roles.remove(roleType);
          }
        });
      },
    );
  }

  bool hasEditPermission(Group group, String aid) {
    final agr = group.applicationRoles.firstWhere(
        (item) => item.applicationId == aid && item.groupId == group.id,
        orElse: () => null);
    if (agr == null || !agr.roles.contains(ApplicationRoleType.FEATURE_EDIT)) {
      return false;
    }
    return true;
  }

  Group addEditPermission(Group group, String aid) {
    if (!hasEditPermission(group, aid)) {
      final agr = ApplicationGroupRole()
        ..applicationId = aid
        ..groupId = group.id
        ..roles.add(ApplicationRoleType.FEATURE_EDIT);
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
    environments.forEach((environment) {
      var egr = group.environmentRoles.firstWhere(
          (environmentRole) => environmentRole.environmentId == environment.id,
          orElse: () => null);
      egr == null ? egr = EnvironmentGroupRole() : {};
      egr.environmentId = environment.id;
      egr.groupId = group.id;
      egr.roles ??= [];
      retMap[environment.id] = egr;
    });
    return retMap;
  }
}
