import 'package:app_singleapp/api/client_api.dart';
import 'package:flutter/material.dart';

import 'FHFlatButton.dart';
import 'fh_alert_dialog.dart';
import 'fh_outline_button.dart';

typedef DeleteThingFunction = Future<bool> Function();

// when deleting a thing, call this as it provides the generic functionality.
// it must be provided with a callback and either a part or whole message.
class FHDeleteThingWarningWidget extends StatelessWidget {
  final ManagementRepositoryClientBloc bloc;
  final DeleteThingFunction deleteSelected;
  final String thing;
  final String wholeWarning;
  final bool extraWarning;
  final String content;

  const FHDeleteThingWarningWidget(
      {Key key,
      @required this.deleteSelected,
      this.thing,
      this.wholeWarning,
      bool extraWarning,
      @required this.bloc,
      this.content})
      : extraWarning = extraWarning ?? false,
        assert(bloc !=
            null), // must be passed because alert dialogs are passed in overlays which no longer have access to the bloc
        assert(deleteSelected != null),
        assert(thing != null || wholeWarning != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHAlertDialog(
      title: Row(
        children: <Widget>[
          _WarningWidget(
            extra: extraWarning,
          ),
          Text(wholeWarning ?? 'Are you sure you want to delete the ${thing}?',
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
        FHOutlineButton(
          title: 'Cancel',
          onPressed: () {
            bloc.removeOverlay();
          },
        ),
        FHFlatButton(
            title: 'Delete',
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

  const _WarningWidget({Key key, bool extra})
      : extra = extra ?? false,
        super(key: key);

  @override
  Widget build(BuildContext context) {
    final size = extra ? 50.0 : 40.0;
    final iconSize = extra ? 48.0 : 36.0;
    return Container(
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
