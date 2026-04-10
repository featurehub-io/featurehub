import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:openapi_dart_common/openapi.dart';

import 'group_bloc.dart';

class GroupUpdateDialogWidget extends StatefulWidget {
  final Group? group;
  final GroupBloc bloc;

  const GroupUpdateDialogWidget({super.key, required this.bloc, this.group});

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
      _groupName.text = widget.group!.name;
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Form(
      key: _formKey,
      child: FHAlertDialog(
        title: Text(widget.group == null ? l10n.createNewGroup : l10n.editGroup),
        content: SizedBox(
          width: 500,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              TextFormField(
                  autofocus: true,
                  controller: _groupName,
                  decoration: InputDecoration(labelText: l10n.groupNameLabel),
                  validator: ((v) {
                    if (v == null || v.isEmpty) {
                      return l10n.groupNameRequired;
                    }
                    if (v.length < 4) {
                      return l10n.groupNameTooShort;
                    }
                    return null;
                  })),
            ],
          ),
        ),
        actions: <Widget>[
          FHFlatButtonTransparent(
            title: l10n.cancel,
            keepCase: true,
            onPressed: () {
              widget.bloc.mrClient.removeOverlay();
            },
          ),
          FHFlatButton(
              title: widget.group == null ? l10n.create : l10n.update,
              onPressed: () => _handleSubmitted(l10n))
        ],
      ),
    );
  }

  void _handleSubmitted(AppLocalizations l10n) {
    if (_formKey.currentState!.validate()) {
      _callUpdateGroup(_groupName.text).then((onValue) {
        // force list update
        widget.bloc.mrClient.removeOverlay();
        widget.bloc.mrClient.addSnackbar(Text(
            widget.group == null
                ? l10n.groupCreated(_groupName.text)
                : l10n.groupUpdated(_groupName.text)));
      }).catchError((e, s) async {
        if (e is ApiException && e.code == 409) {
          widget.bloc.mrClient.removeOverlay();
          widget.bloc.mrClient.customError(
              messageTitle: l10n.groupAlreadyExists(_groupName.text));
        } else {
          await widget.bloc.mrClient.dialogError(e, s);
        }
      });
    }
  }

  Future<void> _callUpdateGroup(String name) {
    final groupName = name.trim();
    return widget.group == null ? widget.bloc.createGroup(groupName) : widget.bloc.updateGroup(widget.group!, name: groupName);
  }
}

class GroupDeleteDialogWidget extends StatelessWidget {
  final Group group;
  final GroupBloc bloc;

  const GroupDeleteDialogWidget(
      {super.key, required this.group, required this.bloc});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      thing: "group '${group.name}'",
      content: l10n.groupDeleteContent,
      deleteSelected: () async {
        try {
          await bloc.deleteGroup(group.id, true);
          bloc.mrClient.addSnackbar(Text(l10n.groupDeleted(group.name)));
          return true;
        } catch (e, s) {
          if (e is ApiException && e.code >= 400) {
            bloc.mrClient.customError(
                messageTitle: l10n.couldNotDeleteGroup(group.name));
          } else {
            await bloc.mrClient.dialogError(e, s);
          }
          return false;
        }
      },
    );
  }
}
