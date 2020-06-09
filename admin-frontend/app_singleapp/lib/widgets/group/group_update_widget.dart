import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_alert_dialog.dart';
import 'package:app_singleapp/widgets/common/fh_delete_thing.dart';
import 'package:app_singleapp/widgets/common/fh_outline_button.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:openapi_dart_common/openapi.dart';

import 'group_bloc.dart';

class GroupUpdateDialogWidget extends StatefulWidget {
  final Group group;
  final GroupBloc bloc;

  const GroupUpdateDialogWidget({Key key, @required this.bloc, this.group})
      : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _GroupUpdateDialogWidgetState();
  }
}

class _GroupUpdateDialogWidgetState extends State<GroupUpdateDialogWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _groupName = TextEditingController();

  @override
  void initState() {
    super.initState();
    if (widget.group != null) {
      _groupName.text = widget.group.name;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: FHAlertDialog(
        title: Text(widget.group == null ? 'Create new group' : 'Edit group'),
        content: Container(
          width: 500,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              TextFormField(
                  controller: _groupName,
                  decoration: InputDecoration(labelText: 'Group name'),
                  validator: ((v) {
                    if (v.isEmpty) {
                      return 'Please enter a group name';
                    }
                    if (v.length < 4) {
                      return 'Group name needs to be at least 4 characters long';
                    }
                    return null;
                  })),
            ],
          ),
        ),
        actions: <Widget>[
          FHOutlineButton(
            title: 'Cancel',
            onPressed: () {
              widget.bloc.mrClient.removeOverlay();
            },
          ),
          FHFlatButton(
              title: widget.group == null ? 'Create' : 'Update',
              onPressed: (() {
                if (_formKey.currentState.validate()) {
                  _callUpdateGroup(_groupName.text).then((onValue) {
                    // force list update
                    widget.bloc.mrClient.removeOverlay();
                    widget.bloc.mrClient.addSnackbar(Text(
                        "Group '${_groupName.text}' ${widget.group == null ? " created" : " updated"}!"));
                  }).catchError((e, s) async {
                    if (e is ApiException && e.code == 409) {
                      widget.bloc.mrClient.removeOverlay();
                      widget.bloc.mrClient.customError(
                          messageTitle:
                              "Group '${_groupName.text}' already exists");
                    } else {
                      widget.bloc.mrClient.dialogError(e, s);
                    }
                  });
                }
              }))
        ],
      ),
    );
  }

  Future<void> _callUpdateGroup(String name) {
    Group group;
    widget.group != null ? group = widget.group : group = Group();
    group.name = name.trim();
    return widget.group == null
        ? widget.bloc.createGroup(group)
        : widget.bloc.updateGroup(group);
  }
}

class GroupDeleteDialogWidget extends StatelessWidget {
  final Group group;
  final GroupBloc bloc;

  const GroupDeleteDialogWidget(
      {Key key, @required this.group, @required this.bloc})
      : assert(group != null),
        assert(bloc != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      thing: "group '${group.name}'",
      content:
          'All permissions belonging to this group will be deleted \n\nThis cannot be undone!',
      deleteSelected: () async {
        try {
          await bloc.deleteGroup(group.id, true);
          bloc.mrClient.addSnackbar(Text("Group '${group.name}' deleted!"));
          return true;
        } catch (e, s) {
          if (e is ApiException && e.code >= 400) {
            bloc.mrClient.customError(
                messageTitle: 'Could not delete group ${group.name}');
          } else {
            bloc.mrClient.dialogError(e, s);
          }
          return false;
        }
      },
    );
  }
}
