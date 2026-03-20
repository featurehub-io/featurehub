import 'package:flutter/material.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
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
    final l10n = AppLocalizations.of(context)!;
    FHAnalytics.sendScreenView("application-strategy-list");
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        const SizedBox(height: 8.0),
        Wrap(
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            FHHeader(
              title: l10n.applicationStrategies,
              children: [
                FHExternalLinkWidget(
                  tooltipMessage: l10n.viewDocumentation,
                  link:
                      "https://docs.featurehub.io/featurehub/latest/application-strategies.html",
                  icon: const Icon(Icons.arrow_outward_outlined),
                  label: l10n.applicationStrategiesDocumentation,
                )
              ],
            ),
          ],
        ),
        const SizedBox(height: 8.0),
        const FHPageDivider(),
        const SizedBox(height: 8.0),
        const ApplicationStrategyList()
      ],
    );
  }
}
