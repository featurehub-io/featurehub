import 'package:open_admin_app/utils/browser/browser.dart' as browser;

import 'package:flutter/material.dart';

class FHExternalLinkOutlinedWidget extends StatelessWidget {
  final String tooltipMessage;
  final String label;
  final String link;
  final Icon icon;

  const FHExternalLinkOutlinedWidget(
      {super.key,
      required this.tooltipMessage,
      required this.label,
      required this.link,
      required this.icon});

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
            browser.openWindow(link, 'new tab');
          },
          label: icon,
        ),
      ),
    ]);
  }
}
