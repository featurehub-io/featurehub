import 'package:flutter/material.dart';

import 'package:open_admin_app/widgets/common/fh_external_link_widget.dart';

class ExternalDocsLinksWidget extends StatelessWidget {
  const ExternalDocsLinksWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return const Row(children: [
      FHExternalLinkWidget(
          label: 'Docs',
          tooltipMessage: 'Documentation',
          link: 'https://docs.featurehub.io',
          icon: Icon(Icons.arrow_outward_outlined)),
      FHExternalLinkWidget(
          label: 'GitHub',
          tooltipMessage: 'GitHub',
          link: 'https://github.com/featurehub-io/featurehub',
          icon: Icon(Icons.arrow_outward_outlined)),
    ]);
  }
}
