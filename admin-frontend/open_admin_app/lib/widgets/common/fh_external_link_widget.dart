import 'dart:html'; // ignore: avoid_web_libraries_in_flutter

import 'package:flutter/material.dart';

class FHExternalLinkWidget extends StatelessWidget {
  final String tooltipMessage;
  final String label;
  final String link;
  final Icon icon;

  const FHExternalLinkWidget(
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
        child: TextButton.icon(
            icon: icon,
            onPressed: () {
              window.open(link, 'new tab');
            },
            label: Text(
              label,
            )),
      ),
    ]);
  }
}
