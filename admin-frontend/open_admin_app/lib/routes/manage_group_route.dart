import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/third_party/chips_input.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';
import 'package:open_admin_app/widgets/group/group_bloc.dart';
import 'package:open_admin_app/widgets/group/group_update_widget.dart';

/// Every user has access to portfolios, they can only see the ones they have access to
/// and their access will be limited based on whether they are a site admin.
class ManageGroupRoute extends StatefulWidget {
  final bool createGroup;

  const ManageGroupRoute({Key? key, required this.createGroup})
      : super(key: key);

  @override
  ManageGroupRouteState createState() => ManageGroupRouteState();
}

class ManageGroupRouteState extends State<ManageGroupRoute> {
  GroupBloc? bloc;
  bool sortToggle = true;
  int sortColumnIndex = 0;

  @override
  void initState() {
    bloc = BlocProvider.of<GroupBloc>(context);
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    final bloc = this.bloc!;
    FHAnalytics.sendScreenView("group-management");
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Container(
            padding: const EdgeInsets.fromLTRB(0, 0, 30, 10),
            child: const FHHeader(
              title: 'Manage group members',
            )),
        const FHPageDivider(),
        Row(
          children: <Widget>[
            Flexible(
              flex: 1,
              child: StreamBuilder<List<Group>>(
                  stream:
                      bloc.mrClient.streamValley.currentPortfolioGroupsStream,
                  builder: (context, snapshot) {
                    if (!snapshot.hasData) {
                      return Container(
                          padding: const EdgeInsets.all(8),
                          child: const Text('Fetching Groups...'));
                    } else {
                      return Container(
                        padding: const EdgeInsets.only(top: 24, left: 8),
                        child: _groupsDropdown(snapshot.data, bloc),
                      );
                    }
                  }),
            ),
            Flexible(
              flex: 2,
              child: bloc.mrClient.isPortfolioOrSuperAdminForCurrentPid()
                  ? Padding(
                      padding: const EdgeInsets.only(top: 24.0, left: 16),
                      child: Row(
                        children: [
                          Flexible(flex: 1, child: _getAdminActions(bloc)),
                          Flexible(
                            flex: 4,
                            child: FloatingActionButton.extended(
                              icon: const Icon(Icons.add),
                              label: const Text('Create new group'),
                              onPressed: () => _createGroup(bloc),
                            ),
                          ),
                        ],
                      ),
                    )
                  : Container(),
            )
          ],
        ),
        Padding(
          padding: const EdgeInsets.only(top: 10.0),
          child: StreamBuilder<Group?>(
              stream: bloc.groupLoaded,
              builder: (context, snapshot) {
                if (snapshot.hasData) {
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      if (bloc.mrClient
                          .isPortfolioOrSuperAdmin(snapshot.data!.portfolioId!))
                        Padding(
                          padding: const EdgeInsets.all(8.0),
                          child: FilledButton.icon(
                            icon: const Icon(Icons.add),
                            label: const Text('Add members'),
                            onPressed: () => bloc.mrClient
                                .addOverlay((BuildContext context) {
                              return AddMembersDialogWidget(
                                bloc: bloc,
                                group: snapshot.data!,
                              );
                            }),
                          ),
                        ),
                      Card(
                          child: SingleChildScrollView(
                        scrollDirection: Axis.horizontal,
                        child: SelectionArea(
                          child: DataTable(
                            showCheckboxColumn: false,
                            sortAscending: sortToggle,
                            sortColumnIndex: sortColumnIndex,
                            columns: [
                              DataColumn(
                                  label: const Text('Name'),
                                  onSort: (columnIndex, ascending) {
                                    onSortColumn(snapshot.data!.members,
                                        columnIndex, ascending);
                                  }),
                              DataColumn(
                                label: const Text('Email'),
                                onSort: (columnIndex, ascending) {
                                  onSortColumn(snapshot.data!.members,
                                      columnIndex, ascending);
                                },
                              ),
                              DataColumn(
                                label: const Text(
                                    'Type (User or Admin Service Account)'),
                                onSort: (columnIndex, ascending) {
                                  onSortColumn(snapshot.data!.members,
                                      columnIndex, ascending);
                                },
                              ),
                              DataColumn(
                                  label: const Padding(
                                    padding: EdgeInsets.only(left: 12.0),
                                    child: Text('Actions'),
                                  ),
                                  onSort: (i, a) => {}),
                            ],
                            rows: [
                              for (Person member in snapshot.data!.members)
                                DataRow(cells: [
                                  DataCell(
                                    Text(member.name ?? ''),
                                  ),
                                  DataCell(Text(
                                      member.personType == PersonType.person
                                          ? member.email!
                                          : "")),
                                  DataCell(Text(
                                      member.personType == PersonType.person
                                          ? 'User'
                                          : 'Service Account')),
                                  DataCell(bloc.mrClient
                                          .isPortfolioOrSuperAdmin(
                                              snapshot.data!.portfolioId!)
                                      ? Tooltip(
                                          message: "Remove from group",
                                          child: FHIconButton(
                                            icon: const Icon(Icons.delete),
                                            onPressed: () {
                                              try {
                                                bloc.removeFromGroup(
                                                    snapshot.data!, member);
                                                bloc.mrClient.addSnackbar(Text(
                                                    "'${member.name}' removed from group '${snapshot.data!.name}'"));
                                              } catch (e, s) {
                                                bloc.mrClient.dialogError(e, s);
                                              }
                                            },
                                          ),
                                        )
                                      : const Text(''))
                                ])
                            ],
                          ),
                        ),
                      ))
                    ],
                  );
                } else {
                  return Container();
                }
              }),
        ),
      ],
    );
  }

  void onSortColumn(List<Person> people, int columnIndex, bool ascending) {
    setState(() {
      if (columnIndex == 0) {
        if (ascending) {
          people.sort((a, b) {
            return a.name!.toLowerCase().compareTo(b.name!.toLowerCase());
          });
        } else {
          people.sort((a, b) {
            return b.name!.toLowerCase().compareTo(a.name!.toLowerCase());
          });
        }
      }
      if (columnIndex == 1) {
        if (ascending) {
          people.sort((a, b) =>
              a.email!.toLowerCase().compareTo(b.email!.toLowerCase()));
        } else {
          people.sort((a, b) =>
              b.email!.toLowerCase().compareTo(a.email!.toLowerCase()));
        }
      }
      if (columnIndex == 2) {
        if (ascending) {
          people.sort((a, b) => a.personType
              .toString()
              .toLowerCase()
              .compareTo(b.personType.toString().toLowerCase()));
        } else {
          people.sort((a, b) => b.personType
              .toString()
              .toLowerCase()
              .compareTo(a.personType.toString().toLowerCase()));
        }
      }
      if (sortColumnIndex == columnIndex) {
        sortToggle = !sortToggle;
      }
      sortColumnIndex = columnIndex;
    });
  }

  Widget _getAdminActions(GroupBloc bloc) {
    return Row(children: <Widget>[
      StreamBuilder<Group?>(
          stream: bloc.groupLoaded,
          builder: (context, snapshot) {
            if (snapshot.hasData) {
              return Row(children: <Widget>[
                FHIconButton(
                    icon: const Icon(Icons.edit),
                    onPressed: () => bloc.mrClient.addOverlay(
                        (BuildContext context) => GroupUpdateDialogWidget(
                              bloc: bloc,
                              group: bloc.group!,
                            ))),
                //hide the delete button for Admin groups
                snapshot.data!.admin!
                    ? Container()
                    : FHIconButton(
                        icon: const Icon(Icons.delete),
                        onPressed: () =>
                            bloc.mrClient.addOverlay((BuildContext context) {
                              return GroupDeleteDialogWidget(
                                bloc: bloc,
                                group: bloc.group!,
                              );
                            }))
              ]);
            } else {
              return Container();
            }
          }),
    ]);
  }

  Widget _groupsDropdown(List<Group>? groups, GroupBloc bloc) {
    return groups == null || groups.isEmpty
        ? const Text('No groups found in the portfolio')
        : Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.end,
            children: <Widget>[
              Text(
                'Portfolio groups',
                style: Theme.of(context).textTheme.bodySmall,
              ),
              InkWell(
                mouseCursor: SystemMouseCursors.click,
                child: Container(
                  constraints: const BoxConstraints(maxWidth: 300),
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
                    items: groups.map((Group group) {
                      return DropdownMenuItem<String>(
                          value: group.id,
                          child: Text(group.name,
                              style: Theme.of(context).textTheme.bodyMedium,
                              overflow: TextOverflow.ellipsis));
                    }).toList(),
                    hint: Text(
                      'Select group',
                      style: Theme.of(context).textTheme.titleSmall,
                    ),
                    onChanged: (value) {
                      setState(() {
                        bloc.getGroup(value?.toString());
                        bloc.groupId = value?.toString();
                      });
                    },
                    value: bloc.groupId,
                  ),
                ),
              ),
            ],
          );
  }

  @override
  void didUpdateWidget(ManageGroupRoute oldWidget) {
    super.didUpdateWidget(oldWidget);
    _createGroupCheck();
  }

  void _createGroupCheck() {
    if (widget.createGroup && bloc != null) {
      WidgetsBinding.instance.addPostFrameCallback((timeStamp) {
        _createGroup(bloc!);
      });
    }
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _createGroupCheck();
  }

  _createGroup(GroupBloc bloc) {
    bloc.mrClient.addOverlay((BuildContext context) {
      return GroupUpdateDialogWidget(
        bloc: bloc,
      );
    });
  }
}

class AddMembersDialogWidget extends StatefulWidget {
  final Group group;
  final GroupBloc bloc;

  const AddMembersDialogWidget(
      {Key? key, required this.bloc, required this.group})
      : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _AddMembersDialogWidgetState();
  }
}

class _AddMembersDialogWidgetState extends State<AddMembersDialogWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final membersToAdd = <Person>[];

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: FHAlertDialog(
        title: Text('Add members to group ${widget.group.name}'),
        content: SizedBox(
          width: 500,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[membersChips(widget.bloc)],
          ),
        ),
        actions: <Widget>[
          FHFlatButtonTransparent(
            title: 'Cancel',
            keepCase: true,
            onPressed: () {
              widget.bloc.mrClient.removeOverlay();
            },
          ),
          FHFlatButton(
              title: 'Add to group',
              keepCase: true,
              onPressed: () async {
                final group = widget.group;
                group.members = List.from(group.members)..addAll(membersToAdd);
                // remove duplicates
                group.members = group.members.toSet().toList();
                final success = await widget.bloc.updateGroup(group);
                if (success) {
                  widget.bloc.mrClient.removeOverlay();
                  widget.bloc.mrClient
                      .addSnackbar(Text("Group '${group.name}' updated!"));
                }
              })
        ],
      ),
    );
  }

  Widget membersChips(GroupBloc bloc) {
    return ChipsInput(
      initialValue: const [],
      // none, but we could
      decoration:
          const InputDecoration(labelText: 'Enter members to add to group...'),
      findSuggestions: (String query) async {
        if (query.isNotEmpty) {
          var sp = await bloc.mrClient.personServiceApi
              .findPeople(filter: query, order: SortOrder.ASC);
          return sp.people;
        }
        return const <Person>[];
      },
      onChanged: (data) {
        for (var person in data) {
          person = person as Person;
          membersToAdd.add(person);
        }
      },
      // when we need to build a chip because it has been selected, this is what is used
      // it can include an image, so we should perhaps consider this?
      chipBuilder: (context, state, p) {
        final person = p as Person;
        return Padding(
          padding: const EdgeInsets.only(top: 8.0),
          child: InputChip(
            key: ObjectKey(p),
            label: Text('${person.name} (${person.email})'),
            onDeleted: () {
              state.deleteChip(p);
              membersToAdd.remove(p);
            },
            materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
          ),
        );
      },
      // this is what is used to show the suggestions.
      suggestionBuilder: (context, state, p) {
        final person = p as Person;
        return ListTile(
          key: ObjectKey(p),
          title: Text(person.name ?? ''),
          subtitle: Text(person.email ?? ''),
          onTap: () => state.selectSuggestion(p),
        );
      },
    );
  }
}
