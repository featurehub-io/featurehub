import 'package:universal_html/html.dart';

import 'package:flutter/material.dart';

class FHExternalLinkOutlinedWidget extends StatelessWidget {
  final String tooltipMessage;
  final String label;
  final String link;
  final Icon icon;

  const FHExternalLinkOutlinedWidget(
      {Key? key,
      required this.tooltipMessage,
      required this.label,
      required this.link,
      required this.icon})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Row(children: [
      Tooltip(
        message: tooltipMessage,
        child: OutlinedButton.icon(
          icon: Text(
            label,
          ),
          onPressed: () {
            window.open(link, 'new tab');
          },
          label: icon,
        ),
      ),
    ]);
  }
}
