import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_widget.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_footer_button_bar.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
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
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const FHLoadingIndicator();
          } else if (snapshot.connectionState == ConnectionState.active ||
              snapshot.connectionState == ConnectionState.done) {
            if (snapshot.hasError) {
              return const FHLoadingError();
            } else if (snapshot.hasData) {
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
                              style: Theme.of(context).textTheme.bodySmall,
                            ),
                            _GroupsDropdown(groups: snapshot.data!, bloc: bloc),
                          ],
                        ),
                        const SizedBox(width: 16.0),
                        FHUnderlineButton(
                          title: 'Go to manage group members',
                          onPressed: () {
                            ManagementRepositoryClientBloc.router
                                .navigateTo(context, '/groups', params: {
                              'id': [bloc.selectedGroup!]
                            });
                          },
                        ),
                        const FHExternalLinkWidget(
                          tooltipMessage: "View documentation",
                          link:
                              "https://docs.featurehub.io/featurehub/latest/users.html#_group_permissions",
                          icon: Icon(Icons.arrow_outward_outlined),
                          label: 'Group Permissions Documentation',
                        ),
                      ],
                    ),
                    _GroupPermissionDetailWidget(bloc: bloc, mr: mrBloc)
                  ]);
            }
          }
          return const SizedBox.shrink();
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
                  style: Theme.of(context).textTheme.bodyMedium,
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

class _AdminFeatureRole {
  String id;
  String name;
  List<ApplicationRoleType> roles;

  _AdminFeatureRole(this.id, this.name, this.roles);

  bool matches(List<ApplicationRoleType> matchRoles) {
    return roles.length == matchRoles.length &&
        !roles.none((role) => matchRoles.contains(role));
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is _AdminFeatureRole &&
          runtimeType == other.runtimeType &&
          id == other.id;

  @override
  int get hashCode => id.hashCode;

  @override
  String toString() {
    return "id: $id, name: $name, roles: $roles";
  }
}

final _adminFeatureRoles = [
  _AdminFeatureRole('none', 'No feature permissions', []),
  _AdminFeatureRole(
      'creator', 'Create features', [ApplicationRoleType.FEATURE_CREATE]),
  _AdminFeatureRole('editor', 'Create / Edit / Delete features', [
    ApplicationRoleType.FEATURE_CREATE,
    ApplicationRoleType.FEATURE_EDIT_AND_DELETE
  ])
];

final _noFeaturePermissionRole = _adminFeatureRoles[0];
final _editorFeaturePermissionRole = _adminFeatureRoles[2];

_AdminFeatureRole _discoverAdminRoleType(
    Group currentGroup, String applicationId) {
  final roles = currentGroup.applicationRoles
          .firstWhereOrNull((element) => element.applicationId == applicationId)
          ?.roles ??
      [];
  if (roles.length == 1 && roles.contains(ApplicationRoleType.FEATURE_EDIT)) {
    return _editorFeaturePermissionRole;
  }
  return _adminFeatureRoles
          .firstWhereOrNull((adminRole) => adminRole.matches(roles)) ??
      _noFeaturePermissionRole;
}

class _GroupPermissionDetailState extends State<_GroupPermissionDetailWidget> {
  Map<String, EnvironmentGroupRole> newEnvironmentRoles = {};
  Group? currentGroup;
  String? applicationId;
  _AdminFeatureRole? adminFeatureRole;
  _AdminFeatureRole? originalAdminFeatureRole;

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<ApplicationGroupRoles?>(
        stream: widget.bloc.groupRoleStream,
        builder: (context, groupSnapshot) {
          if (!groupSnapshot.hasData) {
            return Container(
                padding: const EdgeInsets.all(20),
                child: const SelectableText(
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
                          child: const SelectableText(
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
                  adminFeatureRole = _discoverAdminRoleType(
                      currentGroup!, widget.bloc.applicationId!);
                  originalAdminFeatureRole = adminFeatureRole;
                }

                final rows = <TableRow>[];
                rows.add(getHeader());
                for (var env in envSnapshot.data!) {
                  rows.add(TableRow(children: [
                    Padding(
                      padding: const EdgeInsets.all(8.0),
                      child: SelectableText(env.name),
                    ),
                    PermissionsCheckbox(
                        envId: env.id,
                        newEnvironmentRoles: newEnvironmentRoles,
                        roleType: RoleType.READ),
                    PermissionsCheckbox(
                        envId: env.id,
                        newEnvironmentRoles: newEnvironmentRoles,
                        roleType: RoleType.LOCK),
                    PermissionsCheckbox(
                        envId: env.id,
                        newEnvironmentRoles: newEnvironmentRoles,
                        roleType: RoleType.UNLOCK),
                    PermissionsCheckbox(
                        envId: env.id,
                        newEnvironmentRoles: newEnvironmentRoles,
                        roleType: RoleType.CHANGE_VALUE),
                    if (widget.bloc.mrClient.identityProviders
                        .featurePropertyExtendedDataEnabled)
                      PermissionsCheckbox(
                        envId: env.id,
                        newEnvironmentRoles: newEnvironmentRoles,
                        roleType: RoleType.EXTENDED_DATA,
                      ),
                  ]));
                }

                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    const SizedBox(height: 24),
                    SelectableText('Set feature level permissions',
                        style: Theme.of(context).textTheme.bodySmall),
                    // SizedBox(height: 4.0),
                    Row(
                      children: <Widget>[
                        DropdownButton<_AdminFeatureRole>(
                          icon: const Padding(
                            padding: EdgeInsets.only(left: 8.0),
                            child: Icon(
                              Icons.keyboard_arrow_down,
                              size: 18,
                            ),
                          ),
                          items: _adminFeatureRoles.map((role) {
                            return DropdownMenuItem<_AdminFeatureRole>(
                                value: role,
                                child: Text(
                                  role.name,
                                  style: Theme.of(context).textTheme.bodyMedium,
                                  overflow: TextOverflow.ellipsis,
                                ));
                          }).toList(),
                          isDense: true,
                          // isExpanded: true,
                          value: adminFeatureRole,
                          onChanged: currentGroup?.admin != true
                              ? (value) =>
                                  setState(() => adminFeatureRole = value)
                              : null,
                        ),
                      ],
                    ),
                    Center(
                      child: Container(
                          padding: const EdgeInsets.fromLTRB(0, 32, 0, 8),
                          child: SelectableText(
                              'Set feature value level permissions per environment',
                              style: Theme.of(context).textTheme.bodySmall)),
                    ),
                    Card(
                        child: Table(
                            defaultVerticalAlignment:
                                TableCellVerticalAlignment.middle,
                            border: TableBorder(
                                horizontalInside: BorderSide(
                                    color: Theme.of(context)
                                        .dividerColor
                                        .withOpacity(0.5))),
                            children: rows)),
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
                          onPressed: () async {
                            final newList = <EnvironmentGroupRole>[];
                            newEnvironmentRoles.forEach((key, value) {
                              newList.add(value);
                            });
                            var newGroup = currentGroup!;
                            newGroup.environmentRoles = newList;
                            if (adminFeatureRole != null &&
                                originalAdminFeatureRole != null &&
                                originalAdminFeatureRole?.id !=
                                    adminFeatureRole?.id) {
                              replaceGroupRoles(newGroup, applicationId!,
                                  originalAdminFeatureRole!, adminFeatureRole!);
                            }
                            await widget.bloc
                                .updateGroupWithEnvironmentRoles(
                                    newGroup.id, newGroup)
                                .then((group) {
                              currentGroup = group;
                              originalAdminFeatureRole = _discoverAdminRoleType(
                                  currentGroup!, widget.bloc.applicationId!);
                              widget.bloc.mrClient.addSnackbar(Text(
                                  "Group '${group?.name ?? '<unknown>'}' updated!"));
                            }).catchError((e, s) {
                              widget.bloc.mrClient.dialogError(e, s);
                            });
                            widget.bloc.mrClient.streamValley.triggerRocket();
                          },
                          title: 'Update'),
                    ])
                  ],
                );
              });
        });
  }

  TableRow getHeader() {
    var headerStyle = Theme.of(context)
        .textTheme
        .titleSmall!
        .copyWith(fontWeight: FontWeight.bold);
    return TableRow(children: [
      const Text(
        '',
      ),
      Center(
          child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Text(
          'Read',
          style: headerStyle,
        ),
      )),
      Center(
          child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Text(
          'Lock',
          style: headerStyle,
        ),
      )),
      Center(
          child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Text(
          'Unlock',
          style: headerStyle,
        ),
      )),
      Center(
          child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Text(
          'Change value / Retire',
          style: headerStyle,
        ),
      )),
      if (widget
          .bloc.mrClient.identityProviders.featurePropertyExtendedDataEnabled)
        Center(
            child: Padding(
          padding: const EdgeInsets.all(12.0),
          child: Text(
            'Read Extended Feature Data',
            style: headerStyle,
          ),
        )),
    ]);
  }

  bool hasEditPermission(Group group, String aid) {
    final agr = group.applicationRoles.firstWhereOrNull(
        (item) => item.applicationId == aid && item.groupId == group.id);

    if (agr == null ||
        !(agr.roles.contains(ApplicationRoleType.FEATURE_EDIT) ||
            agr.roles.contains(ApplicationRoleType.FEATURE_EDIT_AND_DELETE))) {
      return false;
    }
    return true;
  }

  Group addEditPermission(Group group, String aid) {
    if (!hasEditPermission(group, aid)) {
      final agr = ApplicationGroupRole(
          applicationId: aid,
          groupId: group.id,
          roles: [
            ApplicationRoleType.FEATURE_EDIT_AND_DELETE,
            ApplicationRoleType.FEATURE_CREATE
          ]);
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
            environmentId: environment.id, groupId: group.id, roles: []);
      } else {
        egr.environmentId = environment.id;
        egr.groupId = group.id;
      }

      retMap[environment.id] = egr;
    }
    return retMap;
  }

  void replaceGroupRoles(
      Group newGroup,
      String appId,
      _AdminFeatureRole originalAdminFeatureRole,
      _AdminFeatureRole adminFeatureRole) {
    final agr = newGroup.applicationRoles.firstWhereOrNull(
        (appGroupRole) => appGroupRole.applicationId == appId);
    if (agr != null) {
      // these are the roles they have
      final roles = [...agr.roles];
      // if the new feature
      roles.removeWhere((role) =>
          originalAdminFeatureRole.roles.contains(role) ||
          adminFeatureRole.roles.contains(role));
      roles.addAll(adminFeatureRole.roles);
      agr.roles = roles;
    } else {
      newGroup.applicationRoles.add(ApplicationGroupRole(
          applicationId: appId,
          groupId: newGroup.id,
          roles: adminFeatureRole.roles));
    }
  }
}

class PermissionsCheckbox extends StatefulWidget {
  final Map<String, EnvironmentGroupRole> newEnvironmentRoles;
  final String envId;
  final RoleType roleType;
  const PermissionsCheckbox(
      {Key? key,
      required this.envId,
      required this.newEnvironmentRoles,
      required this.roleType})
      : super(key: key);

  @override
  State<PermissionsCheckbox> createState() => _PermissionsCheckboxState();
}

class _PermissionsCheckboxState extends State<PermissionsCheckbox> {
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: Checkbox(
        value: widget.newEnvironmentRoles.containsKey(widget.envId) &&
            widget.newEnvironmentRoles[widget.envId]!.roles
                .contains(widget.roleType),
        onChanged: (value) {
          setState(() {
            if (value == true) {
              widget.newEnvironmentRoles[widget.envId]!.roles
                  .add(widget.roleType);
            } else {
              widget.newEnvironmentRoles[widget.envId]!.roles
                  .remove(widget.roleType);
            }
          });
        },
      ),
    );
  }
}
