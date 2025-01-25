import 'package:flutter/material.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/widgets/application-strategies/application_strategy_list.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_external_link_widget.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';

class ApplicationStrategyRoute extends StatelessWidget {
  final bool createApp;
  const ApplicationStrategyRoute({Key? key, required this.createApp})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    FHAnalytics.sendScreenView("application-strategy-list");
    return const Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        SizedBox(height: 8.0),
        Wrap(
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            FHHeader(
              title: 'Application strategies',
              children: [
                FHExternalLinkWidget(
                  tooltipMessage: "View documentation",
                  link:
                      "https://docs.featurehub.io/featurehub/latest/application-strategies.html",
                  icon: Icon(Icons.arrow_outward_outlined),
                  label: 'Application Strategies Documentation',
                )
              ],
            ),
          ],
        ),
        SizedBox(height: 8.0),
        FHPageDivider(),
        SizedBox(height: 8.0),
        ApplicationStrategyList()
      ],
    );
  }
}
