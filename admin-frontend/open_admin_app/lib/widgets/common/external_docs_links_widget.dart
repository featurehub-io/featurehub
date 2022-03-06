import 'dart:html';

import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';

class ExternalDocsLinksWidget extends StatelessWidget {
  const ExternalDocsLinksWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Row(children: [
      Tooltip(
        message: 'Documentation',
        child: TextButton.icon(
            icon: Icon(Feather.external_link,
                color: Theme.of(context).colorScheme.onPrimary),
            onPressed: () {
              window.open('https://docs.featurehub.io', 'new tab');
            },
            label: Text(
              'Docs',
              style: TextStyle(
                color: Theme.of(context).colorScheme.onPrimary,
              ),
            )),
      ),
      Tooltip(
        message: 'GitHub',
        child: TextButton.icon(
            icon: Icon(AntDesign.github,
                color: Theme.of(context).colorScheme.onPrimary),
            onPressed: () {
              window.open(
                  'https://github.com/featurehub-io/featurehub', 'new tab');
            },
            label: Text(
              'GitHub',
              style: TextStyle(
                color: Theme.of(context).colorScheme.onPrimary,
              ),
            )),
      ),
    ]);
  }
}
