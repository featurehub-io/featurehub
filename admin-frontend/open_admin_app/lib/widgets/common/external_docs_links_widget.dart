import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_widget.dart';

class ExternalDocsLinksWidget extends StatelessWidget {
  const ExternalDocsLinksWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Row(children: [
      FHExternalLinkWidget(label: 'Docs', tooltipMessage: 'Documentation',
        link: 'https://docs.featurehub.io', icon: Icon(Feather.external_link,
            color: Theme.of(context).colorScheme.onPrimary)),
      FHExternalLinkWidget(label: 'GitHub', tooltipMessage: 'GitHub', link: 'https://github.com/featurehub-io/featurehub',
      icon: Icon(AntDesign.github,
          color: Theme.of(context).colorScheme.onPrimary)),
    ]);
  }
}
