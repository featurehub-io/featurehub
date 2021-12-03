import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';

import 'fh_alert_dialog.dart';
import 'fh_flat_button_transparent.dart';

typedef DeleteThingFunction = Future<bool> Function();

// when deleting a thing, call this as it provides the generic functionality.
// it must be provided with a callback and either a part or whole message.
class FHDeleteThingWarningWidget extends StatelessWidget {
  final ManagementRepositoryClientBloc bloc;
  final DeleteThingFunction deleteSelected;
  final String? thing;
  final String? wholeWarning;
  final bool extraWarning;
  final String? content;
  final bool isResetThing;

  const FHDeleteThingWarningWidget(
      {Key? key,
      required this.deleteSelected,
      this.thing,
      this.wholeWarning,
      this.extraWarning = false,
      required this.bloc,
      this.content, this.isResetThing = false})
      : assert(thing != null || wholeWarning != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHAlertDialog(
      title: Row(
        children: <Widget>[
          _WarningWidget(
            extra: extraWarning,
          ),
          Text(wholeWarning ?? 'Are you sure you want to delete the $thing?',
              style: TextStyle(color: extraWarning ? Colors.red : null)),
        ],
      ),
      content: Row(
        children: <Widget>[
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: Text(content ?? 'This cannot be undone!'),
          ),
        ],
      ),
      actions: <Widget>[
        // usually buttons at the bottom of the dialog
        FHFlatButtonTransparent(
          title: 'Cancel',
          keepCase: true,
          onPressed: () {
            bloc.removeOverlay();
          },
        ),
        FHFlatButton(
            title: isResetThing ? 'Reset' : 'Delete',
            onPressed: () async {
              if (await deleteSelected()) {
                bloc.removeOverlay();
              }
            })
      ],
    );
  }
}

class _WarningWidget extends StatelessWidget {
  final bool extra;

  const _WarningWidget({Key? key, this.extra = false}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final size = extra ? 50.0 : 40.0;
    final iconSize = extra ? 48.0 : 36.0;
    return SizedBox(
      width: size,
      height: size,
      child: Icon(
        Icons.warning,
        size: iconSize,
        color: Colors.red,
      ),
    );
  }
}
