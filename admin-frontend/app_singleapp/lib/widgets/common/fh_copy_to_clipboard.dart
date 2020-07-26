import 'dart:html' as html;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

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
    return IconButton(
      mouseCursor: SystemMouseCursors.click,
      icon: Icon(Icons.content_copy, size: 16.0),
      tooltip: tooltipMessage,
      onPressed: () async {
        await html.window.navigator.permissions
            .query({'name': 'clipboard-write'});
        await html.window.navigator.clipboard.writeText(copyString);
      },
    );
  }
}
