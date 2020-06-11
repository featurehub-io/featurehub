import 'dart:html' as html;

import 'package:app_singleapp/utils/custom_cursor.dart';
import 'package:flutter/material.dart';

class FHCopyToClipboard extends StatelessWidget {
  const FHCopyToClipboard({
    Key key,
    @required this.tooltipMessage,
    @required this.copyString,
  }) : super(key: key);

  final String tooltipMessage;
  final String copyString;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(left: 16.0),
      child: Tooltip(
        message: tooltipMessage,
        child: CustomCursor(
          child: InkWell(
            child: Icon(Icons.content_copy, size: 16.0),
            onTap: () async {
              await html.window.navigator.permissions
                  .query({'name': 'clipboard-write'});
              await html.window.navigator.clipboard.writeText(copyString);
            },
          ),
        ),
      ),
    );
  }
}
