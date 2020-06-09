import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/common/fh_footer_button_bar.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import 'manage_app_bloc.dart';

class GroupPermissionsWidget extends StatefulWidget {
  const GroupPermissionsWidget({Key key}) : super(key: key);

  @override
  _GroupPermissionState createState() => _GroupPermissionState();
}

class _GroupPermissionState extends State<GroupPermissionsWidget> {
  String selectedGroup;

  @override
  Widget build(BuildContext context) {
    ManageAppBloc bloc = BlocProvider.of(context);
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

          if (selectedGroup == null && snapshot.data.length > 0) {
            selectedGroup = snapshot.data[0].id;
            bloc.getGroupRoles(selectedGroup);
          }

          return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Container(
                    padding: EdgeInsets.only(left: 10, top: 20),
                    child: Text(
                      'Group',
                      style: Theme.of(context).textTheme.caption,
                    )),
                Container(
                    padding: EdgeInsets.fromLTRB(10, 0, 0, 5),
                    child: groupsDropdown(snapshot.data, bloc)),
                _GroupPermissionDetailWidget(bloc: bloc, mr: mrBloc)
              ]);
        });
  }

  Widget groupsDropdown(List<Group> groups, ManageAppBloc bloc) {
    return Container(
      child: DropdownButton(
        items: groups.map((Group group) {
          return DropdownMenuItem<String>(
              value: group.id,
              child: Text(
                group.name,
              ));
        }).toList(),
        hint: Text('Select group'),
        onChanged: (value) {
          setState(() {
            selectedGroup = value;
            bloc.getGroupRoles(value);
          });
        },
        value: selectedGroup,
      ),
    );
  }
}

class _GroupPermissionDetailWidget extends StatefulWidget {
  final ManagementRepositoryClientBloc mr;
  final ManageAppBloc bloc;

  const _GroupPermissionDetailWidget({
    Key key,
    @required this.mr,
    @required this.bloc,
  })  : assert(mr != null),
        assert(bloc != null),
        super(key: key);

  @override
  _GroupPermissionDetailState createState() => _GroupPermissionDetailState();
}

class _GroupPermissionDetailState extends State<_GroupPermissionDetailWidget> {
  Map<String, EnvironmentGroupRole> newEnvironmentRoles =
      Map<String, EnvironmentGroupRole>();
  Group currentGroup;
  bool editAccess;

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<Group>(
        stream: widget.bloc.groupRoleStream,
        builder: (context, groupSnapshot) {
          if (!groupSnapshot.hasData) {
            return Container();
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

                List<TableRow> rows = List();
                rows.add(getHeader());
                for (Environment env in envSnapshot.data) {
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
                        getPermissionCheckbox(env.id, RoleType.EDIT),
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
                                this.editAccess = value;
                              });
                            }),
                        Text(
                            'This group can create, edit and delete features for this application',
                            style: Theme.of(context).textTheme.caption),
                        Padding(
                          padding: const EdgeInsets.only(left: 32.0),
                          child: FHFlatButtonTransparent(
                            title: 'Manage group members',
                            keepCase: true,
                            onPressed: () {
                              ManagementRepositoryClientBloc.router.navigateTo(
                                  context, '/manage-group',
                                  replace: true,
                                  transition: TransitionType.material,
                                  params: {
                                    'id': [currentGroup.id]
                                  });
                            },
                          ),
                        ),
                      ],
                    ),
                    Container(
                        padding: EdgeInsets.fromLTRB(5, 10, 0, 15),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.start,
                          children: <Widget>[
                            Text(
                                'Set the group access to features for each environment',
                                style: Theme.of(context).textTheme.caption),
                          ],
                        )),
                    Table(children: rows),
                    FHButtonBar(children: [
                      FHFlatButtonTransparent(
                          onPressed: () {
                            this.currentGroup = null;
                            widget.bloc.resetGroup(groupSnapshot.data);

                            widget.bloc.mrClient.addSnackbar(Text(
                                "Group '${groupSnapshot.data.name}' reset!"));
                          },
                          title: 'Undo'),
                      FHFlatButton(
                          onPressed: () {
                            List<EnvironmentGroupRole> newList = List();
                            this.newEnvironmentRoles.forEach((key, value) {
                              newList.add(value);
                            });
                            Group newGroup = groupSnapshot.data;
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
                                .catchError(widget.bloc.mrClient.dialogError);
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
              'Environment',
              style: Theme.of(context).textTheme.subtitle2,
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
      value: this.newEnvironmentRoles[envId].roles.contains(roleType),
      onChanged: (value) {
        setState(() {
          if (value) {
            this.newEnvironmentRoles[envId].roles.add(roleType);
          } else {
            this.newEnvironmentRoles[envId].roles.remove(roleType);
          }
        });
      },
    );
  }

  bool hasEditPermission(Group group, String aid) {
    ApplicationGroupRole agr = group.applicationRoles.firstWhere(
        (item) => item.applicationId == aid && item.groupId == group.id,
        orElse: () => null);
    if (agr == null || !agr.roles.contains(ApplicationRoleType.FEATURE_EDIT)) {
      return false;
    }
    return true;
  }

  Group addEditPermission(Group group, String aid) {
    if (!hasEditPermission(group, aid)) {
      ApplicationGroupRole agr = ApplicationGroupRole()
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
    Map<String, EnvironmentGroupRole> retMap =
        Map<String, EnvironmentGroupRole>();
    environments.forEach((environment) {
      EnvironmentGroupRole egr = group.environmentRoles.firstWhere(
          (environmentRole) => environmentRole.environmentId == environment.id,
          orElse: () => null);
      egr == null ? egr = EnvironmentGroupRole() : {};
      egr.environmentId = environment.id;
      egr.groupId = group.id;
      if (egr.roles == null) {
        egr.roles = List<RoleType>();
      }
      retMap[environment.id] = egr;
    });
    return retMap;
  }
}
